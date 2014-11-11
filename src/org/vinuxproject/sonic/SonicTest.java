// This file was written by me, Bill Cox in 2011.
// I place this file into the public domain.  Feel free to copy from it.
// Note, however that libsonic, which this application links to,
// is licensed under LGPL.  You can link to it in your commercial application,
// but any changes you make to sonic.c or sonic.h need to be shared under
// the LGPL license.

package org.vinuxproject.sonic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SonicTest extends Activity {
	public static int SAMPLING_FREQUENCY = 44100;
	public static int CHANNEL_COUNT = 2;
//	public static final int SAMPLING_FREQUENCY = 22050;
	float speed = 1.0f;
	private PlayerThread mPlayer = null;
	
	byte[] audioArray = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.v("SonicTest", "onCreate");

		final SeekBar sk= (SeekBar) findViewById(R.id.seekBar1);     
		sk.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override       
			public void onStopTrackingTouch(SeekBar seekBar) {      
				// TODO Auto-generated method stub      
			}       

			@Override       
			public void onStartTrackingTouch(SeekBar seekBar) {     
				// TODO Auto-generated method stub      
			}       

			@Override       
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				speed = progress / 50f;
				Log.v("SonicTest", "speed: " + speed);
			}       
		});  
		
		if (mPlayer == null) {
			mPlayer = new PlayerThread();
			Log.v("SonicTest", "starting decoding");
			mPlayer.start();
		}
	}
    
    public void play(View view)
    {
        new Thread(new Runnable() 
        {
            public void run()
            {
            	final EditText pitchEdit = (EditText) findViewById(R.id.pitch);
            	final EditText rateEdit = (EditText) findViewById(R.id.rate);
            	float pitch = Float.parseFloat(pitchEdit.getText().toString());
            	float rate = Float.parseFloat(rateEdit.getText().toString());
                AndroidAudioDevice device = new AndroidAudioDevice(SAMPLING_FREQUENCY, CHANNEL_COUNT);
                Sonic sonic = new Sonic(SAMPLING_FREQUENCY, CHANNEL_COUNT);
                byte samples[] = new byte[4096];
                byte modifiedSamples[] = new byte[2048];
                InputStream soundFile = new ByteArrayInputStream(audioArray);

				if(soundFile != null) {
				    
				    sonic.setPitch(pitch);
				    sonic.setRate(rate);
				    
				    try {
				    	for (int bytesRead = soundFile.read(samples); bytesRead >= 0; bytesRead = soundFile.read(samples)) {
				    		sonic.setSpeed(speed);
				    		
				    		if(bytesRead > 0) {
					        	sonic.putBytes(samples, bytesRead);
					        } else {
							    sonic.flush();
					        }
				        	int available = sonic.availableBytes(); 
				        	if(available > 0) {
				        		if(modifiedSamples.length < available) {
				        		    modifiedSamples = new byte[available*2];
				        		}
				        		sonic.receiveBytes(modifiedSamples, available);
				        		device.writeSamples(modifiedSamples, available);
				        	}
					    }	
				    } catch (IOException e) {
				    	e.printStackTrace();
				    	return;
				    }
				    
				    device.flush();
				}
            }
        } ).start();
    }

	private class PlayerThread extends Thread {
		private MediaExtractor extractor;
		private MediaCodec decoder;

		public PlayerThread() {}

		@Override
		public void run() {
			extractor = new MediaExtractor();
//			try {
//				extractor.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/Download/whatohyeah.mp3");
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.howlingabyss);
			if (afd == null) return;
			try {
				extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				afd.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			for (int i = 0; i < extractor.getTrackCount(); i++) {
				MediaFormat format = extractor.getTrackFormat(i);
				SAMPLING_FREQUENCY = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
				CHANNEL_COUNT = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
				String mime = format.getString(MediaFormat.KEY_MIME);
				if (mime.startsWith("audio/")) {
					extractor.selectTrack(i);
					decoder = MediaCodec.createDecoderByType(mime);
					decoder.configure(format, null, null, 0);
					break;
				}
			}

			if (decoder == null) {
				Log.e("DecodeActivity", "Can't find audio info!");
				return;
			}

			decoder.start();

			ByteBuffer[] inputBuffers = decoder.getInputBuffers();
			ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
			BufferInfo info = new BufferInfo();
			boolean isEOS = false;

			while (true) {
			    if (!isEOS) {
			        int inIndex = decoder.dequeueInputBuffer(10000);
			        if (inIndex >= 0) {
			            ByteBuffer buffer = inputBuffers[inIndex];
			            int sampleSize = extractor.readSampleData(buffer, 0);
			            if (sampleSize < 0) {
			                // We shouldn't stop the playback at this point, just pass the EOS
			                // flag to decoder, we will get it again from the
			                // dequeueOutputBuffer
			                Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
			                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
			                isEOS = true;
			            } else {
			                decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
			                extractor.advance();
			            }
			        }
			    }

			    int outIndex = decoder.dequeueOutputBuffer(info, 10000);
			    switch (outIndex) {
			    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
			        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
			        outputBuffers = decoder.getOutputBuffers();
			        break;
			    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
			        Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
			        break;
			    case MediaCodec.INFO_TRY_AGAIN_LATER:
			        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
			        break;
			    default:
			        ByteBuffer buffer = outputBuffers[outIndex];
			        // How to obtain PCM samples from this buffer variable??
			        byte[] b = new byte[info.size-info.offset];                         
			        int a = buffer.position();
			        buffer.get(b);
			        buffer.position(a);
			        
			        if (audioArray == null) {
			        	audioArray = b;
			        } else {
			        	byte[] c = new byte[audioArray.length + b.length];
			        	System.arraycopy(audioArray, 0, c, 0, audioArray.length);
			        	System.arraycopy(b, 0, c, audioArray.length, b.length);
			        	audioArray = c;
			        }
			        
			        decoder.releaseOutputBuffer(outIndex, true);
			        break;
			    }

			    // All decoded frames have been rendered, we can stop playing now
			    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
			        Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
			        break;
			    }
			}

			decoder.stop();
			decoder.release();
			extractor.release();
		}
	}
}
