/*
 * Copyright 2014 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who 
 * downloaded the software, his/her employer (which must be your employer) and 
 * MbientLab Inc, (the "License").  You may not use this Software unless you 
 * agree to abide by the terms of the License which can be found at 
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge, 
 * that the  Software may not be modified, copied or distributed and can be used 
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other 
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare 
 * derivative works of, modify, distribute, perform, display or sell this 
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE 
 * PROVIDED �AS IS� WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, 
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE, 
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL 
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE, 
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE 
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED 
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST 
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY, 
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY 
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software, 
 * contact MbientLab Inc, at www.mbientlab.com.
 */

/*
 * Copyright (C) 2013 The Android Open Source Project
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
package za.co.paulscott.metawearnanny;

import java.util.HashMap;
import java.util.UUID;

import com.mbientlab.metawear.api.Actions;
import com.mbientlab.metawear.api.Characteristics;
import com.mbientlab.metawear.api.MetaWearBLEService;
import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.MetaWearController.*;
import com.mbientlab.metawear.api.BroadcastReceiverBuilder;
import com.mbientlab.metawear.api.BroadcastReceiverBuilder.*;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Activity controlling the MetaWear board.  This activity is started when 
 * a device is clicked from the list view
 * @author Eric Tsai
 */
public class BLEDeviceController extends Activity {
	public static final String EXTRA_BLE_DEVICE= "BLE_DEVICE";
	
	private final static HashMap<UUID, Integer> views= new HashMap<>();
	
	static {
		views.put(Characteristics.DeviceInformation.MANUFACTURER_NAME.uuid, R.id.manufacturer_name);
		views.put(Characteristics.DeviceInformation.SERIAL_NUMBER.uuid, R.id.serial_number);
		views.put(Characteristics.DeviceInformation.FIRMWARE_VERSION.uuid, R.id.firmware_version);
		views.put(Characteristics.DeviceInformation.HARDWARE_VERSION.uuid, R.id.hardware_version);
	}
	
	private MetaWearBLEService mwService;
	private BluetoothDevice device;
	private HashMap<Integer, SeekBar> seekBars;
	private MetaWearController mwController;
	
	private final ServiceConnection mwServiceConnection = new ServiceConnection() {
		@Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mwService= ((MetaWearBLEService.LocalBinder) service).getService();
            mwController= mwService.getMetaWearController();
            mwService.connect(device);
            mwController.enableNotification(MetaWearController.NotificationRegister.MECHANICAL_SWITCH);
            mwController.readDeviceInformation();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mwService= null;
            mwController= null;
        }
	};
	
	private final BroadcastReceiver metaWearUpdateReceiver= new BroadcastReceiverBuilder()
	.withModuleNotification(new MechanicalSwitch() {
		public void pressed() { ((TextView) findViewById(R.id.mechanical_switch)).setText("Pressed"); }
		public void released() { ((TextView) findViewById(R.id.mechanical_switch)).setText("Released"); }
	}).withModuleNotification(new Temperature() {
		public void receivedTemperature(float degrees) {
			((TextView) findViewById(R.id.temperature))
					.setText(String.format("%.2f C", degrees));
		}
	}).withModuleNotification(new Accelerometer() {
		public void receivedDataValue(short x, short y, short z) { 
			((TextView) findViewById(R.id.accelerometer_data))
					.setText(String.format("(%d, %d, %d)", x, y, z));
		}
	}).build();
	
	private OnItemClickListener actionListener= new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			switch(position) {
			case 0:
			    mwController.setLEDState(LEDState.OFF);
				break;
			case 1:
			    mwController.setLEDState(LEDState.ON);
				break;
			case 2:
			    mwController.setLEDState(LEDState.BLINK);
				break;
			case 3:
			    mwController.setLEDColor((byte)seekBars.get(R.id.ledRedValue).getProgress(), 
				        (byte)seekBars.get(R.id.ledGreenValue).getProgress(),
				        (byte)seekBars.get(R.id.ledBlueValue).getProgress());
				break;
			case 4:
			    mwController.readTemperature();
				break;
			case 5:
			    mwController.enableNotification(MetaWearController.NotificationRegister.ACCELEROMETER_DATA);
	            break;
			case 6:
			    mwController.disableNotification(MetaWearController.NotificationRegister.ACCELEROMETER_DATA);
			    break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bledevice);
		
		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			device= (BluetoothDevice)bundle.getParcelable(EXTRA_BLE_DEVICE);
		}
		
		ListView actionList= (ListView) findViewById(R.id.listView1);
		actionList.setAdapter(new ArrayAdapter<String>(this, 
				R.layout.command_row, R.id.command_name, new String[] {"Turn on LED", "Turn off LED", 
				"Pulse LED", "Set LED Color", "Read Temperature", "Enable Accelerometer Data", 
				"Disable Accelerometer Data"}));
		actionList.setOnItemClickListener(actionListener);
		
		Intent mwIntent= new Intent(this, MetaWearBLEService.class);
		bindService(mwIntent, mwServiceConnection, BIND_AUTO_CREATE);
		
		seekBars= new HashMap<>();
		int[] seekBarIds= {R.id.ledRedValue, R.id.ledGreenValue, R.id.ledBlueValue};
		for(int id: seekBarIds) {
		    SeekBar bar= (SeekBar) findViewById(id);
		    bar.setMax(255);
		    seekBars.put(id, bar);
		}
	}

	private final BroadcastReceiver bluetoothLeReceiver= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
            case Actions.BluetoothLE.CHARACTERISTIC_READ:
                UUID charUuid= (UUID)intent.getExtras().get(BroadcastReceiverBuilder.CHARACTERISTIC_UUID);
                Integer viewId= views.get(charUuid);
                if (viewId != null) {
                    byte[] value= (byte[])intent.getExtras().get(BroadcastReceiverBuilder.CHARACTERISTIC_VALUE);
                    ((TextView) findViewById(viewId))
                            .setText(new String(value));
                }
                break;
            }
        }
	};
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(metaWearUpdateReceiver, MetaWearBLEService.getMetaWearIntentFilter());
		registerReceiver(bluetoothLeReceiver, MetaWearBLEService.getBLEIntentFilter());
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(metaWearUpdateReceiver);
		unregisterReceiver(bluetoothLeReceiver);
	}
	@Override
    protected void onDestroy() {
		super.onDestroy();
		unbindService(mwServiceConnection);
		mwService= null;
	}
	@Override
	protected void onStop() {
		super.onStop();
		mwService.disconnect();
	}
}
