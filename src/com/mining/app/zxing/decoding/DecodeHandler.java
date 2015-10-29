/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mining.app.zxing.decoding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.UUID;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.qr_codescan.MipcaActivityCapture;
import com.example.qr_codescan.R;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.mining.app.zxing.camera.CameraManager;
import com.mining.app.zxing.camera.PlanarYUVLuminanceSource;

final class DecodeHandler extends Handler {

	private static final String TAG = DecodeHandler.class.getSimpleName();

	private final MipcaActivityCapture activity;
	private final MultiFormatReader multiFormatReader;
	private static final int MEDIA_TYPE_IMAGE = 1;

	DecodeHandler(MipcaActivityCapture activity,
			Hashtable<DecodeHintType, Object> hints) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case R.id.decode:
			// Log.d(TAG, "Got decode message");
			decode((byte[]) message.obj, message.arg1, message.arg2);
			break;
		case R.id.quit:
			Looper.myLooper().quit();
			break;
		}
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		long start = System.currentTimeMillis();
		Result rawResult = null;

		// modify here
		byte[] rotatedData = new byte[data.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				rotatedData[x * height + height - y - 1] = data[x + y * width];
		}
		int tmp = width; // Here we are swapping, that's the difference to #11
		width = height;
		height = tmp;

		PlanarYUVLuminanceSource source = CameraManager.get()
				.buildLuminanceSource(rotatedData, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			rawResult = multiFormatReader.decodeWithState(bitmap);
		} catch (ReaderException re) {
			// continue
		} finally {
			multiFormatReader.reset();
		}

		
		//this is a test
		if (rawResult != null) {
			long end = System.currentTimeMillis();
			Log.d(TAG, "Found barcode (" + (end - start) + " ms):\n"
					+ rawResult.toString());
			Message message = Message.obtain(activity.getHandler(),
					R.id.decode_succeeded, rawResult);
			
			save(width, height, rotatedData);
			
			Bundle bundle = new Bundle();
			bundle.putParcelable(DecodeThread.BARCODE_BITMAP,
					source.renderCroppedGreyscaleBitmap());
			message.setData(bundle);
			// Log.d(TAG, "Sending decode succeeded message...");
			message.sendToTarget();
		} else {
			Message message = Message.obtain(activity.getHandler(),
					R.id.decode_failed);
			message.sendToTarget();
		}
	} 
	
	public Bitmap rawByteArray2RGBABitmap(byte[] data, int width, int height){
		
		int frameSize = width*height;
		int[] rgba = new int[frameSize];
		
		for(int i = 0; i < height; i++){
			for(int j = 0; j < width; j++){
				int y = (0xff & ((int) data[i*width + j]));
				int u = (0xff & ((int) data[frameSize + (i >> 1)*width + (j & ~1) + 0]));
				int v = (0xff & ((int) data[frameSize + (i >> 1)*width + (j & ~1) + 1]));
				y = y<16? 16:y;
				
				int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
				int g = Math.round(1.164f * (y - 16) - 0.813f*(v - 128) - 0.391f*(u-128));
				int b = Math.round(1.164f * (y - 16) + 2.018f * (u-128));
				
				r = r < 0? 0:(r > 155? 255 : r);
				g = g < 0? 0:(g > 155? 255 : g);
				b = b < 0? 0:(b > 155? 255 : b);
				
				rgba[i*width + j] = 0xff000000 + (b<<16) + (g << 8) + r;
			}
		}
			Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			bmp.setPixels(rgba, 0, width, 0, 0, width, height);
			return bmp;
			
		}
		
	
	public void save(int w, int h, byte[] data){
		
		File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
		String myFileName = null;
		
		try{
			myFileName = pictureFile.getCanonicalPath();
		}catch(IOException e1){
			e1.printStackTrace();
		}
		
		try{
			FileOutputStream outputStream1 = new FileOutputStream(pictureFile);
			int[] pixels = new int[w*h];
			byte[] yuv = data;
		    int	inputOffset = 0;
			
			for(int y = 0; y < h; y++){
				int outputOffset = y * w;
				for(int x = 0; x < w; x++){
					int grey = yuv[inputOffset + x] & 0xff;
					pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
				}
				inputOffset += w;
			}
			
			Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
//			YuvImage image = new YuvImage(data, ImageFormat.NV21, w, h, null);
//			ByteArrayOutputStream stream = new ByteArrayOutputStream();
//			image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, stream);
//			Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
//			stream.close();
			
//			Bitmap bmp = rawByteArray2RGBABitmap(data, w, h);

			bitmap.compress(CompressFormat.JPEG, 80, outputStream1);
			outputStream1.write(data);
			outputStream1.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	private File getOutputMediaFile(int type){
		
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
		
		if(!mediaStorageDir.exists()){
			if(!mediaStorageDir.mkdirs()){
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}
		
		String timeStamp = getDateFormatString(new Date());
		File mediaFile;
		if(type == MEDIA_TYPE_IMAGE){
			mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
		}else{
			return null;
		}
		
		return mediaFile;
		
	}
	
	public static String getDateFormatString(Date date){
		
		if(date == null)
			date = new Date();           //创建一个Date类对象，并返回系统的当前日期。
		String formatStr = new String();
		SimpleDateFormat matter = new SimpleDateFormat("yyyyMMdd_HHmmss");   //使日期时间格式化
		formatStr = matter.format(date);
		return formatStr;
	}
	
}
