#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>

#include <math.h>

#include <libavutil/imgutils.h>
#include <libavutil/opt.h>
#include <libavcodec/avcodec.h>
#include <libavutil/mathematics.h>
#include <libavutil/samplefmt.h>
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"

#define LOG_TAG "FFChunkedAudioVideoEncoder"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Video Encoding

AVCodec *codec;
AVCodecContext *c= NULL;
AVFrame *frame;
AVPacket pkt;
int i, out_size, x, y, outbuf_size;
FILE *f1, *f2; // f1 is the file used for writing, f2 is a buffer file ready for rapid swapping
uint8_t *outbuf;
int had_output=0;

// Muxing

AVOutputFormat *fmt;
AVFormatContext *oc;
AVStream *audio_st, *video_st;
double audio_pts, video_pts;

const jbyte *native_output_file1;
const jbyte *native_output_file2;

int output_width;
int output_height;

AVCodecContext* initializeAVCodecContext(AVCodecContext *c);

// TESTING
//int64_t last_video_frame_pts;
int DEVICE_FRAME_RATE = 25;  // allow variable frame_rate based on device capabilities
int safe_to_encode = 1; // Ensure no collisions when writing audio / video from separate threads
long first_video_frame_timestamp;
long last_video_frame_timestamp;
long current_video_frame_timestamp;
int last_pts;

/***********************************
 *   AUXILLARY MUXING METHODS      *
 *  from ffmpeg's muxing-example.c *
 ***********************************/
#undef exit

/* 5 seconds stream duration */
#define STREAM_DURATION   5.0
#define STREAM_FRAME_RATE 15 /* 15 images/s UNUSED */
#define STREAM_NB_FRAMES  ((int)(STREAM_DURATION * STREAM_FRAME_RATE)) // UNUSED
#define STREAM_PIX_FMT PIX_FMT_YUV420P /* default pix_fmt */

static int sws_flags = SWS_BICUBIC;

/**************************************************************/
/* audio output */

float t, tincr, tincr2;
float *samples;
uint8_t *audio_outbuf;
int audio_outbuf_size;
int audio_input_frame_size;
int audio_frame_count;

int device_audio_sample_fmt = AV_SAMPLE_FMT_S16; // required for native aac
int codec_audio_sample_fmt = AV_SAMPLE_FMT_FLT;

/* CODEC_IDs */
int AUDIO_CODEC_ID = CODEC_ID_AAC;
int VIDEO_CODEC_ID = CODEC_ID_H264;

/*
 * add an audio output stream
 */
static AVStream *add_audio_stream(AVFormatContext *oc, enum CodecID codec_id)
{
    AVCodecContext *c;
    AVStream *st;
    AVCodec *codec;

    /* find the audio encoder */
	codec = avcodec_find_encoder(AUDIO_CODEC_ID);
	if (!codec) {
		LOGE("add_audio_stream codec not found");
		exit(1);
    }
    st = avformat_new_stream(oc, codec);
    if (!st) {
    	LOGE("Could not alloc stream");
        exit(1);
    }

    c = st->codec;
    c->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;
    c->codec_id = codec_id;
    c->codec_type = AVMEDIA_TYPE_AUDIO;

    /* put sample parameters */
    c->sample_fmt = codec_audio_sample_fmt;
    c->bit_rate = 192000;
    c->sample_rate = 44100;
    c->channels = 1;

    // some formats want stream headers to be separate
    if(oc->oformat->flags & AVFMT_GLOBALHEADER)
        c->flags |= CODEC_FLAG_GLOBAL_HEADER;

    return st;
}

static void open_audio(AVFormatContext *oc, AVStream *st)
{
    AVCodecContext *c;

    c = st->codec;

    // Though I've tested that st->codec->codec_id == CODEC_ID_AAC
	// at this point, avcodec_open2 fails unless I explicitly pass
	// a AVCodec...
	AVCodec *codec;
	/* find the audio encoder */
	codec = avcodec_find_encoder(AUDIO_CODEC_ID);
	if (!codec) {
		LOGE("open_audio codec not found");
		exit(1);
	}
	//LOGI("open_audio stream codec_id: %d", c->codec_id);
	/* open it */
	if (avcodec_open2(c, codec, NULL) < 0) {
		LOGE("open_audio could not open codec");
		exit(1);
	}

    audio_outbuf_size = 10000;
    audio_outbuf = av_malloc(audio_outbuf_size);

    if (c->codec->capabilities & CODEC_CAP_VARIABLE_FRAME_SIZE)
            audio_input_frame_size = 10000;
	else
		audio_input_frame_size = c->frame_size;
	samples = av_malloc(audio_input_frame_size *
						av_get_bytes_per_sample(c->sample_fmt) *
						c->channels);
}

/* prepare a 16 bit dummy audio frame of 'frame_size' samples and
   'nb_channels' channels */
static void get_audio_frame(float *samples, int frame_size, int nb_channels)
{
    int j, i, v;
    int16_t *q;

    q = samples;
    for(j=0;j<frame_size;j++) {
        v = (int)(sin(t) * 10000);
        for(i = 0; i < nb_channels; i++)
            *q++ = v;
        t += tincr;
        tincr += tincr2;
    }
}

static void write_audio_frame(AVFormatContext *oc, AVStream *st)
{
    AVCodecContext *c;
    AVPacket pkt = { 0 }; // data and size must be 0;
    AVFrame *frame = avcodec_alloc_frame();
    int got_packet;

    av_init_packet(&pkt);
    c = st->codec;

    // samples is populated in processAVData(...)

    frame->nb_samples = audio_input_frame_size;
	avcodec_fill_audio_frame(frame, c->channels, c->sample_fmt,
							 (uint8_t *)samples,
							 audio_input_frame_size *
							 av_get_bytes_per_sample(c->sample_fmt) *
							 c->channels, 1);

	avcodec_encode_audio2(c, &pkt, frame, &got_packet);
	if (!got_packet)
		return;

	pkt.stream_index = st->index;

	LOGI("AUDIO_PTS: %" PRId64 " AUDIO_DTS %" PRId64 " duration %d" ,pkt.pts, pkt.dts,pkt.duration); // int64_t. in AVStream->time_base units

	/* Write the compressed frame to the media file. */
	if (av_interleaved_write_frame(oc, &pkt) != 0) {
		fprintf(stderr, "Error while writing audio frame\n");
		exit(1);
	}
}

static void close_audio(AVFormatContext *oc, AVStream *st)
{
    avcodec_close(st->codec);

    av_free(samples);
    av_free(audio_outbuf);
}

/**************************************************************/
/* video output */

AVFrame *picture, *tmp_picture;
uint8_t *video_outbuf;
int video_frame_count, video_outbuf_size;

/* add a video output stream */
static AVStream *add_video_stream(AVFormatContext *oc, enum CodecID codec_id)
{
    AVCodecContext *c;
    AVStream *st;
    AVCodec *codec;

    /* find the video encoder */
    codec = avcodec_find_encoder(codec_id);
    if (!codec) {
	    LOGE("codec not found");
    	//fprintf(stderr, "codec not found\n");
	    exit(1);
    }

    // Adds a stream to oc's output file and initializes the
    // AVCodecContext with codec-specific defaults
    st = avformat_new_stream(oc, codec);

    if (!st) {
        LOGE("could not alloc stream");
    	//fprintf(stderr, "Could not alloc stream\n");
        exit(1);
    }

    c = st->codec;

    avcodec_get_context_defaults3(c, codec);

    c->codec_id = codec_id;
    //c->codec_type = AVMEDIA_TYPE_VIDEO;

    /* put sample parameters */
    c->bit_rate = 400000;
    /* resolution must be a multiple of two */
    c->width = 320;
    c->height = 240;
    /* time base: this is the fundamental unit of time (in seconds) in terms
       of which frame timestamps are represented. for fixed-fps content,
       timebase should be 1/framerate and timestamp increments should be
       identically 1. */
    c->time_base.den = DEVICE_FRAME_RATE;
    c->time_base.num = 1;
    c->gop_size = 12; /* emit one intra frame every twelve frames at most */
    c->pix_fmt = STREAM_PIX_FMT;
    if (c->codec_id == CODEC_ID_MPEG2VIDEO) {
        /* just for testing, we also add B frames */
        c->max_b_frames = 2;
    }
    if (c->codec_id == CODEC_ID_MPEG1VIDEO){
        /* Needed to avoid using macroblocks in which some coeffs overflow.
           This does not happen with normal video, it just happens here as
           the motion of the chroma plane does not match the luma plane. */
        c->mb_decision=2;
    }
    // some formats want stream headers to be separate
    if(oc->oformat->flags & AVFMT_GLOBALHEADER)
        c->flags |= CODEC_FLAG_GLOBAL_HEADER;

    return st;
}

static AVFrame *alloc_picture(enum PixelFormat pix_fmt, int width, int height)
{
    AVFrame *picture;
    uint8_t *picture_buf;
    int size;

    picture = avcodec_alloc_frame();
    if (!picture)
        return NULL;
    size = avpicture_get_size(pix_fmt, width, height);
    picture_buf = av_malloc(size);
    if (!picture_buf) {
        av_free(picture);
        return NULL;
    }
    avpicture_fill((AVPicture *)picture, picture_buf,
                   pix_fmt, width, height);
    return picture;
}

static void open_video(AVFormatContext *oc, AVStream *st)
{
    AVCodecContext *c;

    c = st->codec;

    AVCodec *codec;
    /* find the video encoder */
    codec = avcodec_find_encoder(VIDEO_CODEC_ID);
    if (!codec) {
    		LOGE("open_video codec not found");
    		exit(1);
    }
	//LOGI("open_video stream codec_id: %d", c->codec_id);
	/* open the codec */
	if (avcodec_open2(c, codec, NULL) < 0) {
		LOGE("open_video could not open codec");
		if(c->codec_id == VIDEO_CODEC_ID)
			LOGE("c->codec_id is VIDEO_CODEC_ID");
		//fprintf(stderr, "could not open codec\n");
		exit(1);
	}

    video_outbuf = NULL;
    if (!(oc->oformat->flags & AVFMT_RAWPICTURE)) {
        /* allocate output buffer */
        /* XXX: API change will be done */
        /* buffers passed into lav* can be allocated any way you prefer,
           as long as they're aligned enough for the architecture, and
           they're freed appropriately (such as using av_free for buffers
           allocated with av_malloc) */
        video_outbuf_size = 200000;
        video_outbuf = av_malloc(video_outbuf_size);
    }

    /* allocate the encoded raw picture */
    picture = alloc_picture(c->pix_fmt, c->width, c->height);
    if (!picture) {
        fprintf(stderr, "Could not allocate picture\n");
        exit(1);
    }

    /* if the output format is not YUV420P, then a temporary YUV420P
       picture is needed too. It is then converted to the required
       output format */
    tmp_picture = NULL;
    if (c->pix_fmt != PIX_FMT_YUV420P) {
        tmp_picture = alloc_picture(PIX_FMT_YUV420P, c->width, c->height);
        if (!tmp_picture) {
            fprintf(stderr, "Could not allocate temporary picture\n");
            exit(1);
        }
    }
}

/* prepare a dummy image */
static void fill_yuv_image(AVFrame *pict, int frame_index, int width, int height)
{
    int x, y, i;

    i = frame_index;

    /* Y */
    for(y=0;y<height;y++) {
        for(x=0;x<width;x++) {
            pict->data[0][y * pict->linesize[0] + x] = x + y + i * 3;
        }
    }

    /* Cb and Cr */
    for(y=0;y<height/2;y++) {
        for(x=0;x<width/2;x++) {
            pict->data[1][y * pict->linesize[1] + x] = 128 + y + i * 2;
            pict->data[2][y * pict->linesize[2] + x] = 64 + x + i * 5;
        }
    }
}

static void write_video_frame(AVFormatContext *oc, AVStream *st)
{
    int out_size, ret;
    AVCodecContext *c;
    static struct SwsContext *img_convert_ctx;

    c = st->codec;

    if (oc->oformat->flags & AVFMT_RAWPICTURE) {
        /* raw video case. The API will change slightly in the near
           futur for that */
        AVPacket pkt;
        av_init_packet(&pkt);

        pkt.flags |= AV_PKT_FLAG_KEY;
        pkt.stream_index= st->index;
        pkt.data= (uint8_t *)picture;
        pkt.size= sizeof(AVPicture);

        ret = av_interleaved_write_frame(oc, &pkt);
    } else {
    	//LOGI("Encoding image");
        /* encode the image */
        out_size = avcodec_encode_video(c, video_outbuf, video_outbuf_size, picture);
        /* if zero size, it means the image was buffered */
        if (out_size > 0) {
            AVPacket pkt;
            av_init_packet(&pkt);

            if (c->coded_frame->pts != AV_NOPTS_VALUE)
                pkt.pts= av_rescale_q(c->coded_frame->pts, c->time_base, st->time_base);
            if(c->coded_frame->key_frame)
                pkt.flags |= AV_PKT_FLAG_KEY;

            pkt.stream_index= st->index;
            pkt.data= video_outbuf;
            pkt.size= out_size;
            //LOGI("first_video_frame_time: %ld",first_video_frame_timestamp);

            // Determine video pts by ms time difference since last frame + recorded frame count
            double video_gap = (current_video_frame_timestamp - first_video_frame_timestamp) / ((double) 1000); // seconds
            double time_base = ((double) st->time_base.num) / (st->time_base.den);
            // %ld - long,  %d - int, %f double/float
            LOGI("VIDEO_FRAME_GAP_S: %f TIME_BASE: %f PTS %"  PRId64, video_gap, time_base, (int)(video_gap / time_base));

            int proposed_pts = (int)(video_gap / time_base);
            if(last_pts != -1 && proposed_pts == last_pts){
            	proposed_pts ++;
            }
            pkt.pts = proposed_pts;
            last_pts = proposed_pts;
            //video_frame_count++;

            LOGI("VIDEO_PTS: %" PRId64 " DTS: %" PRId64 " duration %d", pkt.pts, pkt.dts, pkt.duration);
            //last_video_frame_pts = pkt.pts;

            /* write the compressed frame in the media file */
            ret = av_interleaved_write_frame(oc, &pkt);
            LOGI("Wrote interleaved frame");
        } else {
            ret = 0;
        }
    }
    if (ret != 0) {
        LOGE("Error writing video frame");
    	//fprintf(stderr, "Error while writing video frame\n");
        exit(1);
    }
    video_frame_count++;

}

static void close_video(AVFormatContext *oc, AVStream *st)
{
    avcodec_close(st->codec);
    av_free(picture->data[0]);
    av_free(picture);
    if (tmp_picture) {
        av_free(tmp_picture->data[0]);
        av_free(tmp_picture);
    }
    av_free(video_outbuf);
}

/***********************************
 *            END OF
 *   AUXILLARY MUXING METHODS      *
 *  from ffmpeg's muxing-example.c *
 ***********************************/

void Java_net_openwatch_reporter_recording_FFChunkedAudioVideoEncoder_internalInitializeEncoder(JNIEnv * env, jobject this, jstring filename1, jstring filename2, jint width, jint height, jint fps){

	// Convert Java types
	//const jbyte *native_filename1, *native_filename2;
	native_output_file1 = (*env)->GetStringUTFChars(env, filename1, NULL);
	native_output_file2 = (*env)->GetStringUTFChars(env, filename2, NULL);

	DEVICE_FRAME_RATE = (int) fps;

	output_height = (int) height;
	output_width = (int) width;

	LOGI("1. internalInitializeEncoder");

	initializeAVFormatContext();

}

void Java_net_openwatch_reporter_recording_FFChunkedAudioVideoEncoder_shiftEncoders(JNIEnv * env, jobject this, jstring new_filename){
	// Point the hot file to the buffer file
	// Point the new buffer at the given new_filename
	// Must be called after finalizeEncoder();

	while(safe_to_encode != 1) // temp hack
			continue;
	safe_to_encode = 0;
	finalizeAVFormatContext();

	const jbyte *native_new_filename;
	native_new_filename = (*env)->GetStringUTFChars(env, new_filename, NULL);

	// Shift output file
	native_output_file1 = native_output_file2;
	native_output_file2 = native_new_filename;

	initializeAVFormatContext();

	safe_to_encode = 1;
}

void encodeVideoFrame(jbyteArray *native_video_frame_data, jlong this_video_frame_timestamp){
	if(safe_to_encode != 1)
			LOGI("COLLISION!-V");
	while(safe_to_encode != 1) // temp hack
		continue;

	safe_to_encode = 0;

	LOGI("ENCODE-VIDEO-0");

	// If this is the first frame, set current and last frame ts
	// equal to the current frame. Else the new last ts = old current ts
	if(video_frame_count == 0){
		last_video_frame_timestamp = (long) this_video_frame_timestamp;
		first_video_frame_timestamp = last_video_frame_timestamp;
	}else{
		last_video_frame_timestamp = current_video_frame_timestamp;
	}

	current_video_frame_timestamp = (long) this_video_frame_timestamp;


	// write video_frame_data to AVFrame
	if(video_st){
		c = video_st->codec; // don't need to do this each frame?

		for(y=0;y<c->height;y++) {
			for(x=0;x<c->width;x++) {
				picture->data[0][y * picture->linesize[0] + x] = (int)native_video_frame_data[0];
				native_video_frame_data++;
			}
		}

		/* Cb and Cr */
		for(y=0;y<c->height/2;y++) {
			for(x=0;x<c->width/2;x++) {
				picture->data[2][y * picture->linesize[2] + x] = (int)native_video_frame_data[0];
				picture->data[1][y * picture->linesize[1] + x] = (int)native_video_frame_data[1];
				native_video_frame_data+=2;
			}
		}
	}

	/* compute current audio and video time */
	if (audio_st)
		audio_pts = (double)audio_st->pts.val * audio_st->time_base.num / audio_st->time_base.den;
	else
		audio_pts = 0.0;

	if (video_st)
		video_pts = (double)video_st->pts.val * video_st->time_base.num / video_st->time_base.den;
	else
		video_pts = 0.0;

	if(video_pts && audio_pts)
		LOGI("video_pts: %" PRId64 " audio_pts: %" PRId64,video_pts,audio_pts); // stream.pts -> AVFrac ->val -> int64_t
	else
		LOGI("video_pts or audio_pts missing");

	if (!audio_st && !video_st){
		LOGE("No audio OR video stream :(");
			return;
	}

	LOGI("pre write video frame");
	/* write interleaved video frames */
	write_video_frame(oc, video_st);
	//write_audio_frame(oc, audio_st);


	LOGI("ENCODE-VIDEO-1");
	safe_to_encode = 1;
}

void Java_net_openwatch_reporter_recording_FFChunkedAudioVideoEncoder_processAVData(JNIEnv * env, jobject this, jbyteArray video_frame_data, jlong this_video_frame_timestamp, jshortArray audio_data, jint audio_length){
	LOGI("processAVData");

	// VIDEO
	jbyte *native_video_frame_data = (*env)->GetByteArrayElements(env, video_frame_data, NULL);
	//encodeVideoFrame(native_video_frame_data, this_video_frame_timestamp);
	if(safe_to_encode != 1)
				LOGI("COLLISION!-V");
		while(safe_to_encode != 1) // temp hack
			continue;

		safe_to_encode = 0;

		LOGI("ENCODE-VIDEO-0");

		// If this is the first frame, set current and last frame ts
		// equal to the current frame. Else the new last ts = old current ts
		if(video_frame_count == 0){
				last_video_frame_timestamp = (long) this_video_frame_timestamp;
				first_video_frame_timestamp = last_video_frame_timestamp;
			}else{
				last_video_frame_timestamp = current_video_frame_timestamp;
			}

		current_video_frame_timestamp = (long) this_video_frame_timestamp;


		// write video_frame_data to AVFrame
		if(video_st){
			c = video_st->codec; // don't need to do this each frame?

			for(y=0;y<c->height;y++) {
				for(x=0;x<c->width;x++) {
					picture->data[0][y * picture->linesize[0] + x] = native_video_frame_data[0];
					native_video_frame_data++;
				}
			}

			/* Cb and Cr */
			for(y=0;y<c->height/2;y++) {
				for(x=0;x<c->width/2;x++) {
					picture->data[2][y * picture->linesize[2] + x] = native_video_frame_data[0];
					picture->data[1][y * picture->linesize[1] + x] = native_video_frame_data[1];
					native_video_frame_data+=2;
				}
			}
		}

		/* compute current audio and video time */
		if (audio_st)
			audio_pts = (double)audio_st->pts.val * audio_st->time_base.num / audio_st->time_base.den;
		else
			audio_pts = 0.0;

		if (video_st)
			video_pts = (double)video_st->pts.val * video_st->time_base.num / video_st->time_base.den;
		else
			video_pts = 0.0;

		if(video_pts && audio_pts)
			LOGI("video_pts: %" PRId64 " audio_pts: %" PRId64,video_pts,audio_pts); // stream.pts -> AVFrac ->val -> int64_t
		else
			LOGI("video_pts or audio_pts missing");

		if (!audio_st && !video_st){
			LOGE("No audio OR video stream :(");
				return;
		}

		LOGI("pre write video frame");
		/* write interleaved video frames */
		write_video_frame(oc, video_st);
		//write_audio_frame(oc, audio_st);


		LOGI("ENCODE-VIDEO-1");
		safe_to_encode = 1;
	(*env)->ReleaseByteArrayElements(env, video_frame_data, native_video_frame_data, 0);

	LOGI("encodeVideoFrame complete");

	// AUDIO
	if(audio_data == NULL)
		return; // no audio data present
	jshort *native_audio_frame_data = (*env)->GetShortArrayElements(env, audio_data, NULL);

	//encodeAudioFrames(native_audio_frame_data, audio_length);
	if(safe_to_encode != 1)
			LOGI("COLLISION!-A");
		while(safe_to_encode != 1) // temp hack
				continue;
		safe_to_encode = 0;
		//LOGI("ENCODE-AUDIO-0");

		if((int)audio_length % audio_input_frame_size != 0){
			LOGE("Audio length: %d, audio_input_frame_size %d", (int)audio_length, audio_input_frame_size);
			exit(1);
		}

		int num_frames = (int) audio_length / audio_input_frame_size;

		if(audio_st){
			int x = 0;
			for(x=0;x<num_frames;x++){ // for each audio frame
				int audio_sample_count = 0;
				//LOG("Audio frame size: %d", audio_input_frame_size);
				for(y=0;y<audio_input_frame_size;y++){ // copy each sample
					samples[y] = (native_audio_frame_data[0] / 32767.0);
					native_audio_frame_data++;
					audio_sample_count++;
				}
				write_audio_frame(oc, audio_st);
			}
			//LOGI("Audio sample count: %d", audio_sample_count);

			/* write interleaved video frames */

			//LOGI("Write audio frame!");
		}

		//LOGI("ENCODE-AUDIO-1");
		safe_to_encode = 1;
	(*env)->ReleaseShortArrayElements(env, audio_data, native_audio_frame_data, 0);

	LOGI("encodeAudioFrame complete");

}

void encodeAudioFrames(jshortArray * native_audio_frame_data, jint audio_length){
	if(safe_to_encode != 1)
		LOGI("COLLISION!-A");
	while(safe_to_encode != 1) // temp hack
			continue;
	safe_to_encode = 0;
	//LOGI("ENCODE-AUDIO-0");

	if((int)audio_length % audio_input_frame_size != 0){
		LOGE("Audio length: %d, audio_input_frame_size %d", (int)audio_length, audio_input_frame_size);
		exit(1);
	}

	int num_frames = (int) audio_length / audio_input_frame_size;

	if(audio_st){
		int x = 0;
		for(x=0;x<num_frames;x++){ // for each audio frame
			int audio_sample_count = 0;
			//LOG("Audio frame size: %d", audio_input_frame_size);
			for(y=0;y<audio_input_frame_size;y++){ // copy each sample
				samples[y] = (int)(native_audio_frame_data[0]);
				native_audio_frame_data++;
				audio_sample_count++;
			}
			write_audio_frame(oc, audio_st);
		}
		//LOGI("Audio sample count: %d", audio_sample_count);

		/* write interleaved video frames */

		//LOGI("Write audio frame!");
	}

	//LOGI("ENCODE-AUDIO-1");
	safe_to_encode = 1;
}

void Java_net_openwatch_reporter_recording_FFChunkedAudioVideoEncoder_finalizeEncoder(JNIEnv * env, jobject this, jint is_final){
	while(safe_to_encode != 1) // temp hack
				continue;
		safe_to_encode = 0;
	LOGI("finalize file %s", native_output_file1);

	int native_is_final = (int) is_final;

	// if is final finalize
	if(native_is_final != 0){
		unlink(native_output_file2); // remove unused buffer file
	}
	finalizeAVFormatContext();
	safe_to_encode = 1;
}

void finalizeAVFormatContext(){

	/* write the trailer, if any.  the trailer must be written
	 * before you close the CodecContexts open when you wrote the
	 * header; otherwise write_trailer may try to use memory that
	 * was freed on av_codec_close() */
	av_write_trailer(oc);

	// close each codec
	if (video_st)
		close_video(oc, video_st);
	if (audio_st)
		close_audio(oc, audio_st);

	// free the streams
	for(i = 0; i < oc->nb_streams; i++) {
		av_freep(&oc->streams[i]->codec);
		av_freep(&oc->streams[i]);
	}

	if (!(fmt->flags & AVFMT_NOFILE)) {
		// close the output file
		avio_close(oc->pb);
	}

	// free the stream
	av_free(oc);

}

// Method to be called after beginning new chunk
// to initialize AVFormatContext with new chunk filename
int initializeAVFormatContext(){

	video_frame_count = 0;
	last_pts = -1;
	audio_frame_count = 0;

	av_register_all();
	/*
	//TESTING
	AVCodecContext *new_ctx;
	AVCodec *new_codec = avcodec_find_encoder(CODEC_ID_MP2);
	if(!new_codec)
		LOGE("failed to open codec");
	else
		LOGI("found codec!");

	new_ctx = avcodec_alloc_context3(new_codec);

	if(new_ctx == NULL)
		LOGE("Failed to allocate context");

	if (avcodec_open2(new_ctx, new_codec, NULL) == 0)
		LOGI("opened codec!");
	else{
		LOGI("Failed to open codec");
		if(avcodec_open(new_ctx, new_codec) == 0)
			LOGI("Opened codec with avcodec_open");
		else
			LOGE("Failed to open codec with avcodec_open");
	}
	*/
	// END TESTING

	LOGI("2. initializingAVFormatContext with file: %s", native_output_file1);

	/* allocate the output media context */
	avformat_alloc_output_context2(&oc, NULL, NULL, ((const char*) native_output_file1));
	if (!oc) {
		//printf("Could not deduce output format from file extension: using MPEG.\n");
		LOGI("Could not deduce output format from file extension: using MPEG.");
		avformat_alloc_output_context2(&oc, NULL, "mpeg", ((const char*)native_output_file1));
	}
	if (!oc) {
		LOGE("a.0.e Could not init AVFormatContext");
		exit(1);
	}

	// set A/V Codecs
	oc->oformat->video_codec = VIDEO_CODEC_ID;
	oc->oformat->audio_codec = AUDIO_CODEC_ID;

	fmt= oc->oformat;

	LOGI("3. initializeAVFormatContext");

	/* add the audio and video streams using the default format codecs
	   and initialize the codecs */
	video_st = NULL;
	audio_st = NULL;
	if (fmt->video_codec != CODEC_ID_NONE) {
		video_st = add_video_stream(oc, fmt->video_codec);
		LOGI("added video stream");
	}
	if (fmt->audio_codec != CODEC_ID_NONE) {
		audio_st = add_audio_stream(oc, fmt->audio_codec);
		LOGI("added audio stream");
	}

	LOGI("4. set AVFormat codecs");

	/* Now that all the parameters are set, we can open the audio and
	   video codecs and allocate the necessary encode buffers */
	if (video_st)
		open_video(oc, video_st);
	if (audio_st)
		open_audio(oc, audio_st);

	av_dump_format(oc, 0, native_output_file1, 1);

	LOGI("open_video and audio");

	/* open the output file, if needed */
	if (!(fmt->flags & AVFMT_NOFILE)) {
		if (avio_open(&oc->pb, native_output_file1, AVIO_FLAG_WRITE) < 0) {
			//fprintf(stderr, "Could not open '%s'\n", native_filename1);
			LOGE("Could not open '%s'", native_output_file1);
			exit(1);
		}else{
			LOGI("4a. output file opened successfully");
		}
	}


	/* write the stream header, if any */
	//av_write_header(oc);
	avformat_write_header(oc,NULL);
	LOGI("5. write file header");

	LOGI("video frame size: %d, audio frame size: %d", video_st->codec->frame_size, audio_st->codec->frame_size);

	return audio_st->codec->frame_size;
}
