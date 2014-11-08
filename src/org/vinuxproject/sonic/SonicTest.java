// This file was written by me, Bill Cox in 2011.
// I place this file into the public domain.  Feel free to copy from it.
// Note, however that libsonic, which this application links to,
// is licensed under LGPL.  You can link to it in your commercial application,
// but any changes you make to sonic.c or sonic.h need to be shared under
// the LGPL license.

package org.vinuxproject.sonic;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SonicTest extends Activity
{
//	public static final int SAMPLING_FREQUENCY = 44100;
	public static final int SAMPLING_FREQUENCY = 22050;
	float speed = 1.0f;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
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
            // TODO Auto-generated method stub      

        	speed = progress / 50f;
        	Log.v("SonicTest", "speed: " + speed);
//            Toast.makeText(getApplicationContext(), String.valueOf(progress),Toast.LENGTH_SHORT).show();

        }       
    });  
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
                AndroidAudioDevice device = new AndroidAudioDevice(SAMPLING_FREQUENCY, 1);
                Sonic sonic = new Sonic(SAMPLING_FREQUENCY, 1);
                byte samples[] = new byte[4096];
                byte modifiedSamples[] = new byte[2048];
                InputStream soundFile = getResources().openRawResource(R.raw.howlingabyss);

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
}
