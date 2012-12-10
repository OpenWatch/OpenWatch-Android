package net.openwatch.reporter.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

enum OWFileUploadState {
	LOCAL, UPLOADING, SYNCED
}

@Table(name = "OWLocalRecordingSegment")
public class OWLocalRecordingSegment extends Model{
	
	@Column(name = "filepath")
	public String filepath;
	
	@Column(name = "filename")
	public String filename;
	
	@Column(name = "state")
	public OWFileUploadState state;
	
	@Column(name = "recording")
	public OWLocalRecording recording;
	
	
	

}
