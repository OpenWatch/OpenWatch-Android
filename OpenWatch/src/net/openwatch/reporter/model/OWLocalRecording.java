package net.openwatch.reporter.model;

import java.util.List;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "LocalRecordings")
public class OWLocalRecording extends OWRecording {
	
	@Column(name = "filepath")
	public String filepath;
	
	@Column(name = "hq_synced")
	public boolean hq_file_synced = false;
	
	@Column(name = "lq_synced")
	public boolean lq_file_synced = false;

	public List<OWLocalRecordingSegment> segments(){
		return getMany(OWLocalRecordingSegment.class, "Recording");
	}
}
