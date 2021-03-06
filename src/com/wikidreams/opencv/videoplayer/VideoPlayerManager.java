package com.wikidreams.opencv.videoplayer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.wikidreams.opencv.haarcascade.Cascade;
import com.wikidreams.opencv.haarcascade.Classifier;
import com.wikidreams.opencv.haarcascade.Converter;
import com.wikidreams.opencv.templatematching.TemplateMatchManager;

/**
 * @author WIT Software - Daniel Vieira
 * @version 1.0 - 2016
 * <p>This class allows play video contents and use a recognition method.</p>
 */

public class VideoPlayerManager {

	private String frameTitle;
	private CanvasFrame canvas;
	private Frame frame;
	private File video;
	private Cascade cascade;
	private BufferedImage image;
	private int processType;
private int templateMatchMethod;
	
	public VideoPlayerManager(String frameTitle, Cascade cascade, File video) {
		super();
		this.frameTitle = frameTitle;
		if (video.exists()) {
			this.cascade = cascade;
			this.video = video;
			this.processType = 0; // Recognition using Haar cascades.
			this.run();
		}
	}

	public VideoPlayerManager(String frameTitle, File image, File video, int templateMatchMethod) {
		super();
		this.frameTitle = frameTitle;
		
		if (image.exists() && video.exists()) {
			try {
				this.image = ImageIO.read(image);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.templateMatchMethod = templateMatchMethod;
			this.processType = 1;
			this.video = video;
			this.run();
		}
	}


	private void run() {
		this.canvas = new CanvasFrame(this.frameTitle);

		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(this.video);

		try {
			grabber.start();
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
		}

		long frameRate = 33;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {

				while (true) {

					try {
						frame = grabber.grabImage();
						if (frame == null) {       
							break;
						}

						// Choose recognition method.
						switch (processType) {
						case 1: processType = 1;
						templateMatchingRecognition(frame);
						break;
						default: haarCascadeRecognition(frame);
						break;
						}

					} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
						e.printStackTrace();
					}
				}
			}
		};

		Timer timer = new Timer(true);
		timer.schedule(task, 0, frameRate);
	}



	private void haarCascadeRecognition(Frame frame) {
		this.frame = frame;

		// JavaCV converter to BufferedImage
		Java2DFrameConverter converterToBI = new Java2DFrameConverter();

		BufferedImage bufferedImage = null;
		ByteArrayOutputStream baos = null;
		byte[] imageData = null;

		// Converte from JavaCV Frame to BufferedImage
		bufferedImage = converterToBI.convert(this.frame);
		if (bufferedImage != null) {
			baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(bufferedImage, "jpg", baos );
				baos.flush();
				imageData = baos.toByteArray();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (baos != null) {
			try {
				baos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (imageData != null) {
			org.opencv.core.Mat img = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
			if (! img.empty()) {
				org.opencv.core.Mat mRgba=new org.opencv.core.Mat();  
				org.opencv.core.Mat mGrey=new org.opencv.core.Mat();  
				img.copyTo(mRgba);  
				img.copyTo(mGrey);
				// Grayscale conversion
				Imgproc.cvtColor( mRgba, mGrey, Imgproc.COLOR_BGR2GRAY);
				// Equalizes the image for better recognition
				Imgproc.equalizeHist( mGrey, mGrey );
				// Detect how many objects are in the loaded image.
				MatOfRect detections = new MatOfRect();
				this.cascade.getCascadeClassifier().detectMultiScale(mGrey, detections);
				org.opencv.core.Mat output = Classifier.createRectangle(detections, img);
				byte[] imageInBytes = Converter.matToByte(output);
				this.displayOnFrame(Converter.byteToBufferedImage(imageInBytes));
			}
		}
	}


	
		private void templateMatchingRecognition(Frame frame) {
		BufferedImage image = Converter.javacvFrameToBufferedImage(frame);
		BufferedImage result = TemplateMatchManager.run(image, this.image, this.templateMatchMethod);
		this.displayOnFrame(result);
	}

	private void displayOnFrame(BufferedImage image) {
		this.canvas.setCanvasSize(image.getWidth(), image.getHeight());
		this.canvas.showImage(image);		
	}

}
