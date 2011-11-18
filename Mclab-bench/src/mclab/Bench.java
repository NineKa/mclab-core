package mclab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.users.*;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Indexed;

import javax.persistence.*;

class File{
	String name;
	Text content;
	public File(String name, String content){
		this.name=name;
		this.content=new Text(content);
	}
	public String getName() {
		return name;
	}
	public Text getContent() {
		return content;
	}
	public File() {
	}
}
@Indexed
public class Bench {
	String author;
	@Indexed 
	Boolean parsed = false;
	Blob xmlBlob;
	@Id	String name;
	Date date;
	String link;
	String Tags;
	@Embedded ArrayList<File> files;
	private User uploader;
	public void addFile(String name, String content){
		files.add(new File(name, content));
	}
	public String downloadURL(){
		return "http://www.mathworks.com/matlabcentral/fileexchange/"+name+"?controller=file_infos&download=true";
	}
	public User getUploader() {
		return uploader;
	}
	public Bench() {
		files = new ArrayList<File>();
		UserService userService = UserServiceFactory.getUserService();
		uploader = userService.getCurrentUser();
	}
	public String toString(){
		String result = "";
		for (File f: files){
			result= result + " " +f.getName();
		}
		return result;
	}
	public String projectNumber(){
		return name.split("-")[0];
	}
	public static String nameFromFilename(String name){
		String[] parts = name.split("/");
		if (parts.length<3){
			throw new IllegalArgumentException(name);
		}
		return parts[2];
	}
}