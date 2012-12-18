#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include <jni.h>
#include <android/log.h>

#include <libavutil/mathematics.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>

#define LOG_TAG "FFChunkedAudioVideoEncoder"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Media FileNames
jbyte *native_output_file_hq;  // hq file
jbyte *native_output_file_lq1; // lq file being written to
jbyte *native_output_file_lq2; // next lq file to be written to

int output_width_hq;
int output_height_hq;
int output_width_lq;
int output_height_lq;

AVOutputFormat *fmt;
AVFormatContext *oc_lq;
AVFormatContext *oc_hq;
AVStream *audio_st_hq, *audio_st_lq, *video_st_hq, *video_st_lq;
double audio_pts, video_pts;
int i;

// Stream Parameters
#define STREAM_PIX_FMT PIX_FMT_YUV420P
int device_frame_rate = 15;
int device_audio_sample_fmt = AV_SAMPLE_FMT_S16; // required for native aac
int codec_audio_sample_fmt = AV_SAMPLE_FMT_FLT;
//int codec_audio_sample_fmt = AV_SAMPLE_FMT_S16;
int VIDEO_CODEC_ID = CODEC_ID_H264;
int AUDIO_CODEC_ID = CODEC_ID_AAC;
//int AUDIO_CODEC_ID = CODEC_ID_AC3;
//int AUDIO_CODEC_ID = CODEC_ID_MP2;
long chunk_duration_ms = 5000; // 5s

// Audio Parameters
//static int16_t *samples_hq, samples_lq;
static float *samples_hq, samples_lq;
static int audio_input_frame_size;
int num_audio_channels = 1;
int audio_sample_rate = 44100;
int64_t audio_channel_layout = AV_CH_LAYOUT_MONO;
int audio_channel_count = 0;
int audio_bitrate_hq = 64000;
int audio_bitrate_lq = 64000;

// Video Paramteres
static AVFrame *picture_lq, *picture_hq, *tmp_picture;
static uint8_t *video_outbuf;
int video_outbuf_freed = 0; // avoid doubly closing video_outbuf
static int sws_flags = SWS_BICUBIC;
static int video_outbuf_size;
long first_video_frame_timestamp_hq;
long first_video_frame_timestamp_lq;
long current_video_frame_timestamp;
int last_pts_hq; // last frame's PTS. Ensure current frame pts > last_pts
int last_pts_lq;
int video_frame_count_hq;
int video_frame_count_lq;
float video_crf_hq = 18;
float video_crf_lq = 24;

// Safety
int safe_to_encode = 1; // Ensure no collisions writing audio / video, initializing, and finalizing

/*
 * This file facilitates recording two simultaneous A/V streams.
 * One "hq" version to a single file and one chunked "lq" version
 * CAVEATS: The audio sample size of audio_st_hq and audio_st_lq must be equal
 */

////////////////////////////////////////////
//             Video Methods              //
////////////////////////////////////////////

/* Add a video output stream.
 * */
static AVStream *add_video_stream(AVFormatContext *oc, enum CodecID codec_id, int width, int height, float crf)
{
    AVCodecContext *c;
    AVStream *st;
    AVCodec *codec;

    /* find the video encoder */
    codec = avcodec_find_encoder(codec_id);
    if (!codec) {
        LOGE("add_video_stream codec not found");
    	//fprintf(stderr, "codec not found\n");
        exit(1);
    }

    st = avformat_new_stream(oc, codec);
    if (!st) {
        LOGE("add_video_stream could not alloc stream");
    	//fprintf(stderr, "Could not alloc stream\n");
        exit(1);
    }

    c = st->codec;

    avcodec_get_context_defaults3(c, codec);

    //LOGI("start add_video_st fps: %d device_frame_rate: %d", c->time_base.den, device_frame_rate);

    c->codec_id = codec_id;

    /* Put sample parameters. */
    c->bit_rate = 400000;
    /* Resolution must be a multiple of two. */
	c->width    = width;
	c->height   = height;

    /* timebase: This is the fundamental unit of time (in seconds) in terms
     * of which frame timestamps are represented. For fixed-fps content,
     * timebase should be 1/framerate and timestamp increments should be
     * identical to 1. */
    c->time_base.den = device_frame_rate;
    c->time_base.num = 1;
    c->gop_size      = 12; /* emit one intra frame every twelve frames at most */
    c->pix_fmt       = STREAM_PIX_FMT;

    if(codec_id == CODEC_ID_H264){
    	av_opt_set(c->priv_data, "preset", "ultrafast", 0);
    	if(crf)
    		av_opt_set_double(c->priv_data, "crf", crf, 0);
    	else
    		av_opt_set_double(c->priv_data, "crf", 24.0, 0);
    }

    if (c->codec_id == CODEC_ID_MPEG2VIDEO) {
        /* just for testing, we also add B frames */
        c->max_b_frames = 2;
    }
    if (c->codec_id == CODEC_ID_MPEG1VIDEO) {
        /* Needed to avoid using macroblocks in which some coeffs overflow.
         * This does not happen with normal video, it just happens here as
         * the motion of the chroma plane does not match the luma plane. */
        c->mb_decision = 2;
    }
    /* Some formats want stream headers to be separate. */
    if (oc->oformat->flags & AVFMT_GLOBALHEADER)
        c->flags |= CODEC_FLAG_GLOBAL_HEADER;
    //LOGI("end add_video_st fps: %d device_frame_rate: %d", st->codec->time_base.den, device_frame_rate);
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
    size        = avpicture_get_size(pix_fmt, width, height);
    picture_buf = av_malloc(size);
    if (!picture_buf) {
        av_free(picture);
        return NULL;
    }
    avpicture_fill((AVPicture *)picture, picture_buf,
                   pix_fmt, width, height);
    return picture;
}

static void open_video(AVFormatContext *oc, AVStream *st, AVFrame **out_picture)
{
    AVCodecContext *c;
    AVFrame *picture;

    c = st->codec;

    // Though I've tested that st->codec->codec_id == CODEC_ID_H264
    // at this point, avcodec_open2 fails unless I explicitly pass
    // a AVCodec...
    AVCodec *codec;
    /* find the video encoder */
	codec = avcodec_find_encoder(VIDEO_CODEC_ID);
	if (!codec) {
		LOGE("open_video codec not found");
		//fprintf(stderr, "codec not found\n");
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
    // TODO: handle video_outbuf independently
    video_outbuf = NULL;
    if (!(oc->oformat->flags & AVFMT_RAWPICTURE)) {
        /* Allocate output buffer. */
        /* XXX: API change will be done. */
        /* Buffers passed into lav* can be allocated any way you prefer,
         * as long as they're aligned enough for the architecture, and
         * they're freed appropriately (such as using av_free for buffers
         * allocated with av_malloc). */
        video_outbuf_size = 200000;
        video_outbuf      = av_malloc(video_outbuf_size);
        //LOGI("av_malloc video_outbuf");
    }

    /* Allocate the encoded raw picture. */
    picture = alloc_picture(c->pix_fmt, c->width, c->height);
    if (!picture) {
        LOGE("could not allocate picture");
    	//fprintf(stderr, "Could not allocate picture\n");
        exit(1);
    }
    //LOGI("alloc_picture");

    /* If the output format is not YUV420P, then a temporary YUV420P
     * picture is needed too. It is then converted to the required
     * output format. */
    tmp_picture = NULL;
    if (c->pix_fmt != PIX_FMT_YUV420P) {
        tmp_picture = alloc_picture(PIX_FMT_YUV420P, c->width, c->height);
        if (!tmp_picture) {
            LOGE("Could not allocate temporary picture");
        	//fprintf(stderr, "Could not allocate temporary picture\n");
            exit(1);
        }
    }

    *out_picture = picture;
    //LOGI("open_video success");
}

/*
 * oc_hq and st_hq may be NULL.
 *
 * NOTE: assumes the LQ and HQ stream PIX_FMT and time_base are equal
 */
static void write_video_frame(AVFormatContext *oc,  AVStream *st, AVFrame *picture, int *out_last_pts, int *out_video_frame_count, long first_video_frame_timestamp)
{
	int video_frame_count = *out_video_frame_count;
	int last_pts = *out_last_pts;
	//LOGI("write_video_frame get last_pts: %d", last_pts);
	//LOGI("write_video_frame sanity last_pts + 1: %d", last_pts + 1);

	if(oc && st && picture){
		//LOGI("write_video_frame got input!");
	}
	else{
		if(!oc)
			LOGE("write_video_frame no oc");
		if(!st)
			LOGE("write_video_frame no st");
		if(!picture)
			LOGE("write_video_frame no picture");
	}
    int out_size, ret;
    AVCodecContext *c;
    static struct SwsContext *img_convert_ctx;

    c = st->codec;

	if (c->pix_fmt != PIX_FMT_YUV420P) {
		LOGE("write_video pix_fmt not YUV420P");
		/* as we only generate a YUV420P picture, we must convert it
		 * to the codec pixel format if needed
		 * DELETE THIS SECTION */
		if (img_convert_ctx == NULL) {
			img_convert_ctx = sws_getContext(c->width, c->height,
											 PIX_FMT_YUV420P,
											 c->width, c->height,
											 c->pix_fmt,
											 sws_flags, NULL, NULL, NULL);
			if (img_convert_ctx == NULL) {
				LOGE("Cannot initialize the conversion context");
				//fprintf(stderr,
				//		"Cannot initialize the conversion context\n");
				exit(1);
			}
		}
		//fill_yuv_image(tmp_picture, frame_count, c->width, c->height);
		sws_scale(img_convert_ctx, tmp_picture->data, tmp_picture->linesize,
				  0, c->height, picture->data, picture->linesize);

    }

    if (oc->oformat->flags & AVFMT_RAWPICTURE) {
    	LOGE("write_video AVFMT_RAWPICTURE detected");
        /* Raw video case - the API will change slightly in the near
         * future for that. */
        AVPacket pkt;
        av_init_packet(&pkt);

        pkt.flags        |= AV_PKT_FLAG_KEY;
        pkt.stream_index  = st->index;
        pkt.data          = (uint8_t *)picture;
        pkt.size          = sizeof(AVPicture);

        ret = av_interleaved_write_frame(oc, &pkt);
    } else {
    	//LOGI("video frame is YUV420P");
        /* encode the lq image */
        out_size = avcodec_encode_video(c, video_outbuf,
                                        video_outbuf_size, picture);
        /* If size is zero, it means the image was buffered. */
        if (out_size > 0) {
        	//LOGI("out_size > 0");
            AVPacket pkt;
            av_init_packet(&pkt);

            if (c->coded_frame->pts != AV_NOPTS_VALUE)
                pkt.pts = av_rescale_q(c->coded_frame->pts,
                                       c->time_base, st->time_base);
            if (c->coded_frame->key_frame)
                pkt.flags |= AV_PKT_FLAG_KEY;

            pkt.stream_index = st->index;
            pkt.data         = video_outbuf;
            pkt.size         = out_size;

            // Determine video pts by ms time difference since last frame + recorded frame count
			double video_gap = (current_video_frame_timestamp - first_video_frame_timestamp) / ((double) 1000); // seconds
			double time_base = ((double) st->codec->time_base.num) / (st->codec->time_base.den);
			// %ld - long,  %d - int, %f double/float
			//LOGI("VIDEO STREAM fps: %d / %d = %f sec per frame", st->codec->time_base.num, st->codec->time_base.den, time_base);
			//LOGI("VIDEO_FRAME_GAP_S: %f TIME_BASE: %f PTS %"  PRId64, video_gap, time_base, (int)(video_gap / time_base));

			int proposed_pts = (int)(video_gap / time_base);
			if(last_pts != -1 && proposed_pts <= last_pts){
				proposed_pts = last_pts + 1;
			}
			pkt.pts = proposed_pts;
			//last_pts = proposed_pts;
			*out_last_pts = proposed_pts;
			//LOGI("write_video_frame set last_pts: %d (lq: %d hq: %d)", proposed_pts, last_pts_lq, last_pts_hq);
            //LOGI("VIDEO_PTS: %" PRId64 " DTS: %" PRId64 " duration %d", pkt.pts, pkt.dts, pkt.duration);
            /* Write the compressed frame to the media file. */
            ret = av_interleaved_write_frame(oc, &pkt); // Write LQ frame

            //LOGI("av_interleaved_write_frame success");
            video_frame_count++;
            *out_video_frame_count = video_frame_count;
        } else {
        	LOGI("out_size < 0: %d", out_size);
            ret = 0;
        }
        if (ret != 0) {
			LOGE("Error writing video frame");
			exit(1);
        }
    }
}

static void close_video(AVFormatContext *oc, AVStream *st, AVFrame *picture)
{
    avcodec_close(st->codec);
    av_free(picture->data[0]);
    av_free(picture);
    //LOGI("close_video pre free tmp_picture");
    if (tmp_picture) {
        av_free(tmp_picture->data[0]);
        av_free(tmp_picture);
    }
    //LOGI("close_video post free tmp_picture");
    if(video_outbuf_freed != 1){
    	av_free(video_outbuf); // we re use the video buffer for all streams, so it may be all ready freed at this point
    	video_outbuf_freed = 1;
    }
}

////////////////////////////////////////////
//             Audio Methods              //
////////////////////////////////////////////


/*
 * add an audio output stream
 */
static AVStream *add_audio_stream(AVFormatContext *oc, enum CodecID codec_id, int bit_rate)
{
    AVCodecContext *c;
    AVStream *st;
    AVCodec *codec;

    /* find the audio encoder */
    codec = avcodec_find_encoder(codec_id);
    if (!codec) {
        LOGE("add_audio_stream codec not found");
        exit(1);
    }
    //LOGI("add_audio_stream found codec_id: %d",codec_id);
    st = avformat_new_stream(oc, codec);
    if (!st) {
    	LOGE("add_audio_stream could not alloc stream");
        exit(1);
    }

    st->id = 1;

    c = st->codec;
    //LOGI("strict_std_compliance: %d FF_COM_EXP: %d", c->strict_std_compliance, FF_COMPLIANCE_EXPERIMENTAL);
    c->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL; // for native aac support
    /* put sample parameters */
    //c->sample_fmt  = AV_SAMPLE_FMT_FLT;
    c->sample_fmt  = codec_audio_sample_fmt;
    c->bit_rate    = bit_rate;
    //c->bit_rate = 64000;
    c->sample_rate = audio_sample_rate;
    c->channels    = 1;
    //LOGI("add_audio_stream parameters: sample_fmt: %d bit_rate: %d sample_rate: %d", codec_audio_sample_fmt, bit_rate, audio_sample_rate);
    // some formats want stream headers to be separate
    if (oc->oformat->flags & AVFMT_GLOBALHEADER)
        c->flags |= CODEC_FLAG_GLOBAL_HEADER;

    //TESTING
    /* open the codec */

    char error_buffer[90];
    char *error_buffer_ptr = error_buffer;
    int error = avcodec_open2(c, codec, NULL);
	if (error < 0) {
		av_strerror (error, error_buffer_ptr, 90);
		LOGE("add_audio_stream could not open codec %d. Error: %d(%s)", codec->id, error, error_buffer_ptr);
		if(c->codec_id == AUDIO_CODEC_ID)
			LOGE("c->codec_id is AUDIO_CODEC_ID");
		error = avcodec_open2(c, NULL, NULL);
		if (error < 0) {
			av_strerror (error, error_buffer_ptr, 90);
			LOGE("add_audio_stream really coudn't open codec. Error: %d(%s)", error, error_buffer_ptr);
		//fprintf(stderr, "could not open codec\n");
		exit(1);
		}
	}
    //TESTING

    return st;
}

static void open_audio(AVFormatContext *oc, AVStream *st, float **out_samples)
{
    AVCodecContext *c;
    float * samples;

    c = st->codec;

    // Though I've tested that st->codec->codec_id == CODEC_ID_AAC
	// at this point, avcodec_open2 fails unless I explicitly pass
	// a AVCodec...
	AVCodec *codec;
	/* find the audio encoder */
	codec = avcodec_find_encoder(AUDIO_CODEC_ID);
	if (!codec) {
		LOGE("open_audio codec not found");
		//fprintf(stderr, "codec not found\n");
		exit(1);
	}
	//LOGI("open_audio stream codec_id: %d", c->codec_id);
    /* open it */
    if (avcodec_open2(c, codec, NULL) < 0) {
        LOGE("open_audio could not open codec");
    	//fprintf(stderr, "could not open codec\n");
        exit(1);
    }

    if (c->codec->capabilities & CODEC_CAP_VARIABLE_FRAME_SIZE)
        audio_input_frame_size = 10000;
    else
        audio_input_frame_size = c->frame_size;

    //LOGI("open_audio frame_size: %d, audio_input_frame_size: %d", c->frame_size, audio_input_frame_size);
    samples = av_malloc(audio_input_frame_size *
                        av_get_bytes_per_sample(c->sample_fmt) *
                        c->channels);

    *out_samples = samples;
}

static void write_audio_frame(AVFormatContext *oc, AVStream *st, float *samples)
{
    AVCodecContext *c;
    AVPacket pkt = { 0 }; // data and size must be 0;
    AVFrame *frame = avcodec_alloc_frame();
    int got_packet;

    av_init_packet(&pkt);
    c = st->codec;

    //get_audio_frame(samples, audio_input_frame_size, c->channels);
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

    //LOGI("AUDIO_PTS: %" PRId64 " AUDIO_DTS %" PRId64 " duration %d" ,pkt.pts, pkt.dts,pkt.duration);

    /* Write the compressed frame to the media file. */
    if (av_interleaved_write_frame(oc, &pkt) != 0) {
        LOGE("error writing audio frame!");
    	//fprintf(stderr, "Error while writing audio frame\n");
        exit(1);
    }
}

static void close_audio(AVFormatContext *oc, AVStream *st, float *samples)
{
    avcodec_close(st->codec);

    av_free(samples);
}


////////////////////////////////////////////
//      AVFormatContext Management        //
////////////////////////////////////////////

/*
 * Initialize the AVFormatContext
 * Called on encoder initialize and when beginning
 * each new video chunk
 */
int initializeAVFormatContext(AVFormatContext **out_oc, jbyte *output_filename, AVStream **out_video_st, AVFrame **out_picture, int video_width, int video_height, float video_crf, int *out_last_pts, int *out_video_frame_count, AVStream **out_audio_st, float **out_samples, int audio_bitrate){
	AVFormatContext *oc;
	AVStream *video_st;
	AVStream *audio_st;
	AVFrame *picture;
	float *samples;

	// TODO: Can we do this only once?
	/* Initialize libavcodec, and register all codecs and formats. */
	av_register_all();
	//LOGI("initializeAVFC with filename: %s", output_filename);
	if(!oc)
		LOGI("initializeAVFC, oc is properly null");

	/* allocate the output media context */
	avformat_alloc_output_context2(&oc, NULL, NULL, ((const char*) output_filename));
	if (!oc) {
		LOGI("Could not deduce output format, using mpeg");
		//printf("Could not deduce output format from file extension: using MPEG.\n");
		avformat_alloc_output_context2(&oc, NULL, "mpeg", ((const char*) output_filename));
	}
	if (!oc) {
		LOGE("Could not allocate output context");
		exit(1);
	}
	//else
		//LOGI("initializeAVFC, oc appears properly allocated");

	//LOGI("avformat_alloc_output_context2");
	fmt = oc->oformat;

	// Set AVOutputFormat video/audio codec
	fmt->video_codec = VIDEO_CODEC_ID;
	fmt->audio_codec = AUDIO_CODEC_ID;

	/* Add the audio and video streams using the default format codecs
	 * and initialize the codecs. */
	video_st = NULL;
	audio_st = NULL;
	if (fmt->video_codec != CODEC_ID_NONE) {
		video_st = add_video_stream(oc, fmt->video_codec, video_width, video_height, video_crf);
		//(AVFormatContext *oc, enum CodecID codec_id, int width, int height, float crf)
	}
	if (fmt->audio_codec != CODEC_ID_NONE) {
		audio_st = add_audio_stream(oc, fmt->audio_codec, audio_bitrate);
		//static AVStream *add_audio_stream(AVFormatContext *oc, enum CodecID codec_id, int bit_rate)
	}
	//LOGI("add_audio_stream / add_video_stream");
	/* Now that all the parameters are set, we can open the audio and
	 * video codecs and allocate the necessary encode buffers. */
	if (video_st){
		open_video(oc, video_st, &picture);
		//open_video(AVFormatContext *oc, AVStream *st, AVFrame *picture
	}
	if (audio_st){
		open_audio(oc, audio_st ,&samples);
		//open_audio(AVFormatContext *oc, AVStream *st, int16_t *samples)
	}

	av_dump_format(oc, 0, output_filename, 1);

	//LOGI("open audio / video");
	/* open the output file, if needed */
	if (!(fmt->flags & AVFMT_NOFILE)) {
		char *error_buffer_ptr;
		char error_buffer[90];
		error_buffer_ptr = error_buffer;
		//LOGI("pre avio_open2");
		int error = avio_open2(&oc->pb, output_filename, AVIO_FLAG_WRITE, NULL, NULL);
		//LOGI("post avio_open2");
		if ( error < 0) {
			av_strerror (error, error_buffer_ptr, 90);
			LOGE("Could not open %s. Error: %s", output_filename, error_buffer_ptr);
			//fprintf(stderr, "Could not open '%s'\n", native_output_file_lq1);
			exit(-420);
		}
	}

	/* Write the stream header, if any. */
	//LOGI("pre avformat_write_header");
	avformat_write_header(oc, NULL);
	//LOGI("avformat_write_header");
	//LOGI("end initializeAVFC: audio_input_frame_size: %d fps: %d", audio_input_frame_size, video_st->codec->time_base.den);

	// Set results to output arguments
	*out_oc = oc;
	*out_video_st = video_st;
	*out_audio_st = audio_st;
	*out_picture = picture;
	*out_samples = samples;
	*out_last_pts = -1;
	*out_video_frame_count = 0;

	return audio_input_frame_size;
}

int initializeLQAVFormatContext(jbyte* native_output_file){
	//LOGI("initializeLQAVF with file: %s", native_output_file);
	int audio_frame_size = initializeAVFormatContext(&oc_lq, native_output_file, &video_st_lq, &picture_lq, output_width_lq, output_height_lq, video_crf_lq, &last_pts_lq, &video_frame_count_lq, &audio_st_lq, &samples_lq, audio_bitrate_lq);
	return audio_frame_size;
}

int initializeHQAVFormatContext(jbyte* native_output_file){
	//LOGI("initializeHQAVF with file: %s", native_output_file);
	//int audio_frame_size = initializeAVFormatContext(oc_hq, native_output_file, video_st_hq, picture_hq, output_width_hq, output_height_hq, video_crf_hq, audio_st_hq, samples_hq, audio_bitrate_hq);
	int audio_frame_size = initializeAVFormatContext(&oc_hq, native_output_file, &video_st_hq, &picture_hq, output_width_hq, output_height_hq, video_crf_hq, &last_pts_hq, &video_frame_count_hq, &audio_st_hq, &samples_hq, audio_bitrate_hq);
	if(!oc_hq)
			LOGE("initializeLQAVFormatContext: oc_hq null after initializing");
	//else
		//LOGI("oc_hq appears properly allocated outside initializeAV.. scope");
	return audio_frame_size;
}

void finalizeAVFormatContext(AVFormatContext **out_oc, AVStream **out_video_st, AVFrame **out_picture, AVStream **out_audio_st, float **out_samples){
	/* Write the trailer, if any. The trailer must be written before you
	 * close the CodecContexts open when you wrote the header; otherwise
	 * av_write_trailer() may try to use memory that was freed on
	 * av_codec_close(). */

	if(!out_oc || !out_video_st || !out_picture || !out_audio_st || !out_samples)
		LOGE("finalizeAVFormatContext argument is null");

	AVFormatContext *oc = *out_oc;
	AVStream *video_st = *out_video_st;
	AVFrame *picture = *out_picture;
	AVStream *audio_st = *out_audio_st;
	float *samples = *out_samples;


	//LOGI("finalizeAVFormatContext");
	av_write_trailer(oc);
	//LOGI("av_write_trailer");

	/* Close each codec. */
	if (video_st)
		close_video(oc, video_st, picture);
	//LOGI("close_video");
	if (audio_st)
		close_audio(oc, audio_st, samples);
	//LOGI("close_audio");

	//LOGI("close audio / video");

	/* Free the streams. */
	for (i = 0; i < oc->nb_streams; i++) {
		av_freep(&oc->streams[i]->codec);
		av_freep(&oc->streams[i]);
	}

	//LOGI("free streams");

	if (!(fmt->flags & AVFMT_NOFILE))
		/* Close the output file. */
		avio_close(oc->pb);

	//LOGI("avio_close");

	/* free the stream */
	av_free(oc);

	//LOGI("av_free");

	*out_oc = NULL;
	*out_video_st = NULL;
    *out_picture = NULL;
	*out_audio_st = NULL;
	*out_samples = NULL;
}

////////////////////////////////////////////
//           Testing methods              //
////////////////////////////////////////////

void testAudioCodec(){
	AVCodec *codec;
	AVCodecContext *c= NULL;
	av_register_all();

	/* find the MP2 encoder */
	codec = avcodec_find_encoder(AUDIO_CODEC_ID);
	if (!codec) {
		LOGE("testAudioCodec find failed");
		//fprintf(stderr, "codec not found\n");
		//exit(1);
	}

	c = avcodec_alloc_context3(codec);
	//avctx->strict_std_compliance > FF_COMPLIANCE_EXPERIMENTAL
	c->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL; // for native aac support
	c->codec_id = AUDIO_CODEC_ID;
	c->codec_type = AVMEDIA_TYPE_AUDIO;
	/* put sample parameters */
	c->bit_rate = 64000;
	c->sample_rate = audio_sample_rate;
	c->channels = 1;
	c->sample_fmt = AV_SAMPLE_FMT_S16;

	/* open it */
	if (avcodec_open2(c, NULL, NULL) < 0) {
			LOGE("testAudioCodec open failed w/ AVCC only");
			//fprintf(stderr, "could not open codec\n");
			//exit(1);
			if (avcodec_open2(c, codec, NULL) < 0) {
					LOGE("testAudioCodec open failed with AVCC and Codec");
					//fprintf(stderr, "could not open codec\n");
					//exit(1);
			}
		}
/*	else{
		LOGI("testAudioCodec opened!");
	}
*/
}

////////////////////////////////////////////
//             JNI Methods                //
////////////////////////////////////////////

int Java_net_openwatch_reporter_recording_FFChunkedAudioVideoEncoder_internalInitializeEncoder(JNIEnv * env, jobject this, jstring output_file_hq, jstring output_file_lq1, jstring output_file_lq2, jint width, jint height, jint fps){

	while(safe_to_encode != 1) // temp hack
		continue;
	safe_to_encode = 0;
	// Convert Java types
	//const jbyte *native_filename1, *native_filename2;
	native_output_file_hq = (*env)->GetStringUTFChars(env, output_file_hq, NULL);
	native_output_file_lq1 = (*env)->GetStringUTFChars(env, output_file_lq1, NULL);
	native_output_file_lq2 = (*env)->GetStringUTFChars(env, output_file_lq2, NULL);

	device_frame_rate = (int) fps;

	output_height_hq = (int) height;
	output_width_hq = (int) width;

	// for now output width of hq and lq will be equal
	output_height_lq = output_height_hq;
	output_width_lq = output_width_hq;

	//LOGI("1. internalInitializeEncoder(). requested fps: %d", fps);
	safe_to_encode = 1;

	//testAudioCodec();
	audio_channel_count = av_get_channel_layout_nb_channels(audio_channel_layout);

	//allocRecordingStructs(); // alloc structs that need only be done once per recording

	//LOGI("pre initializeHQAVFC");
	int audio_frame_size = initializeHQAVFormatContext(native_output_file_hq);
	//LOGI("pre initializeLQAVFC");
	initializeLQAVFormatContext(native_output_file_lq1);
	//int audio_frame_size = picture_hqFormatContext(oc_hq, native_output_file_hq, video_st_hq, picture_hq, output_width_hq, output_height_hq, video_crf_hq, audio_st_hq, samples_hq, audio_bitrate_hq);
	//initializeAVFormatContext(oc_lq, native_output_file_lq1, video_st_lq, picture_lq, output_width_lq, output_height_lq, video_crf_lq, audio_st_lq, samples_lq, audio_bitrate_lq);
	return audio_frame_size;
}

void Java_net_openwatch_reporter_recording_FFChunkedAudioVideoEncoder_processAVData(JNIEnv * env, jobject this, jbyteArray video_frame_data, jlong this_video_frame_timestamp, jshortArray audio_data, jint audio_length){

	while(safe_to_encode != 1) // temp hack
		continue;

	safe_to_encode = 0;

	//LOGI("processAVData");

	AVCodecContext *c;

	// get video frame data
	jbyte *native_video_frame_data = (*env)->GetByteArrayElements(env, video_frame_data, NULL);

	//LOGI("ENCODE-VIDEO-0");

	// If this is the first frame, set current and last frame ts
	// equal to the current frame. Else the new last ts = old current ts
	if(video_frame_count_lq == 0)
		first_video_frame_timestamp_lq = (long) this_video_frame_timestamp;
	if(video_frame_count_hq == 0)
		first_video_frame_timestamp_hq = (long) this_video_frame_timestamp;

	current_video_frame_timestamp = (long) this_video_frame_timestamp;

	int x,y; // audio, video loop counters
	//LOGI("pre copy HQ frame");
	// write HQ video_frame_data to AVFrame
	if(video_st_hq){
		c = video_st_hq->codec; // don't need to do this each frame?
		for(y=0;y<c->height;y++) {
			for(x=0;x<c->width;x++) {
				picture_hq->data[0][y * picture_hq->linesize[0] + x] = native_video_frame_data[0];
				native_video_frame_data++;
			}
		}

		/* Cb and Cr */
		for(y=0;y<c->height/2;y++) {
			for(x=0;x<c->width/2;x++) {
				picture_hq->data[2][y * picture_hq->linesize[2] + x] = native_video_frame_data[0];
				picture_hq->data[1][y * picture_hq->linesize[1] + x] = native_video_frame_data[1];
				native_video_frame_data+=2;
			}
		}
	}


	/* write interleaved video frames */
	// Currently the same picture is used for video streams
	if(output_width_hq != output_width_lq || output_height_hq != output_height_lq){
		LOGE("HQ and LQ videos have different dimensions. Case not yet supported");
		exit(1);
	}
	//LOGI("pre write LQ video frame");
	write_video_frame(oc_lq, video_st_lq, picture_hq, &last_pts_lq, &video_frame_count_lq, first_video_frame_timestamp_lq);
	//LOGI("post LQ, pre HQ video frame");
	write_video_frame(oc_hq, video_st_hq, picture_hq, &last_pts_hq, &video_frame_count_hq, first_video_frame_timestamp_hq);
	//LOGI("post write HQ video frame");

	(*env)->ReleaseByteArrayElements(env, video_frame_data, native_video_frame_data, 0);

	// AUDIO
	if(audio_data == NULL){
		safe_to_encode = 1;
		return; // no audio data present
	}
	jshort *native_audio_frame_data = (*env)->GetShortArrayElements(env, audio_data, NULL);

	if((int)audio_length % audio_input_frame_size != 0){
		LOGE("Audio length: %d, audio_input_frame_size %d", (int)audio_length, audio_input_frame_size);
		exit(1);
	}

	int num_frames = (int) audio_length / audio_input_frame_size;

	if(audio_st_hq){
		//LOGI("ENCODE_AUDIO-0");

		int x = 0;
		for(x=0;x<num_frames;x++){ // for each audio frame
			int audio_sample_count = 0;
			//LOG("Audio frame size: %d", audio_input_frame_size);
			//TODO: conver samples to float based
			//LOGI("pre audio sample copying");
			for(y=0;y<audio_input_frame_size;y++){ // copy each sample
				samples_hq[y] = (native_audio_frame_data[0] / 32767.0); // convert S16 to FLT. That was refreshingly simple
				//samples_hq[y] = native_audio_frame_data[0];
				native_audio_frame_data++;
				audio_sample_count++;
			}
			// use the same audio samples for both audio streams
			write_audio_frame(oc_hq, audio_st_hq, samples_hq);
			write_audio_frame(oc_lq, audio_st_lq, samples_hq);
		}
		//LOGI("free converted_native...");
	}

	(*env)->ReleaseShortArrayElements(env, audio_data, native_audio_frame_data, 0);

	// Check to see if it's time for chunking
	//if(current_video_frame_timestamp - first_video_frame_timestamp > chunk_duration_ms){
		// shift encoder
	//}
	safe_to_encode = 1;
}

void Java_net_openwatch_reporter_recording_FFChunkedAudioVideoEncoder_shiftEncoders(JNIEnv * env, jobject this, jstring new_filename){
	// Point the hot file to the buffer file
	// Point the new buffer at the given new_filename
	// Must be called after finalizeEncoder();

	while(safe_to_encode != 1) // temp hack
		continue;
	safe_to_encode = 0;

	LOGI("shiftEncoders");
	finalizeAVFormatContext(&oc_lq, &video_st_lq, &picture_lq, &audio_st_lq, &samples_lq);
	//finalizeAVFormatContext(AVFormatContext *oc, AVStream *video_st, AVStream *audio_st)

	const jbyte *native_new_filename;
	native_new_filename = (*env)->GetStringUTFChars(env, new_filename, NULL);

	// Shift output file
	native_output_file_lq1 = native_output_file_lq2;
	native_output_file_lq2 = native_new_filename;

	initializeLQAVFormatContext(native_output_file_lq1);

	safe_to_encode = 1;
}

void Java_net_openwatch_reporter_recording_FFChunkedAudioVideoEncoder_finalizeEncoder(JNIEnv * env, jobject this, jint is_final){

	while(safe_to_encode != 1) // temp hack
		continue;
	safe_to_encode = 0;

	LOGI("finalize file %s", native_output_file_lq1);

	int native_is_final = (int) is_final;

	// if finalizing last video chunk
	if(native_is_final != 0){
		LOGI("finalizing HQ video");
		finalizeAVFormatContext(&oc_hq, &video_st_hq, &picture_hq, &audio_st_hq, &samples_hq);
		unlink(native_output_file_lq2); // delete unused buffer file
		//freeRecordingStructs();
	}
	LOGI("finalizing LQ video");
	//finalizeAVFormatContext(&oc_lq, &video_st_lq, &picture_lq, &audio_st_lq, &samples_lq);
	finalizeAVFormatContext(&oc_lq, &video_st_lq, &picture_lq, &audio_st_lq, &samples_lq);
	safe_to_encode = 1;
}
