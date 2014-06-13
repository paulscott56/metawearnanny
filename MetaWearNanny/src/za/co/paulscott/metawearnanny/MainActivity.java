package za.co.paulscott.metawearnanny;

import java.util.HashMap;
import java.util.UUID;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mbientlab.metawear.api.Actions;
import com.mbientlab.metawear.api.BroadcastReceiverBuilder;
import com.mbientlab.metawear.api.BroadcastReceiverBuilder.Temperature;
import com.mbientlab.metawear.api.Characteristics;
import com.mbientlab.metawear.api.MetaWearBLEService;
import com.mbientlab.metawear.api.MetaWearController;

public class MainActivity extends Activity {

	public static String EXTRA_BLE_DEVICE = "BLE_DEVICE";

	private MetaWearBLEService mwService;
	private BluetoothDevice device;
	private MetaWearController mwController;

	private final static HashMap<UUID, Integer> views = new HashMap<>();

	private static final String TAG = "MainActivity";

	static {
		views.put(Characteristics.DeviceInformation.MANUFACTURER_NAME.uuid,
				R.id.manufacturer_name);
		views.put(Characteristics.DeviceInformation.SERIAL_NUMBER.uuid,
				R.id.serial_number);
		views.put(Characteristics.DeviceInformation.FIRMWARE_VERSION.uuid,
				R.id.firmware_version);
		views.put(Characteristics.DeviceInformation.HARDWARE_VERSION.uuid,
				R.id.hardware_version);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	private final ServiceConnection mwServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mwService = ((MetaWearBLEService.LocalBinder) service).getService();
			mwController = mwService.getMetaWearController();
			mwService.connect(device);
			mwController.readDeviceInformation();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mwService = null;
			mwController = null;
		}
	};

	private final BroadcastReceiver metaWearUpdateReceiver = new BroadcastReceiverBuilder()
			.withModuleNotification(new Temperature() {
				public void receivedTemperature(float degrees) {
					((TextView) findViewById(R.id.temperature)).setText(String
							.format("%.2f C", degrees));
				}
			}).build();

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_scan) {
			Intent launchNewIntent = new Intent(this,
					BLEScannerActivity.class);
			startActivityForResult(launchNewIntent, 1);
		}
		return super.onOptionsItemSelected(item);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				Bundle result = data.getBundleExtra("EXTRA_BLE_DEVICE");
				Log.i(TAG, result.toString());
				//EXTRA_BLE_DEVICE = result;
			}
			if (resultCode == RESULT_CANCELED) {
				Log.e(TAG, "No device results!");
			}
		}
	}

	private final BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getAction()) {
			case Actions.BluetoothLE.CHARACTERISTIC_READ:
				UUID charUuid = (UUID) intent.getExtras().get(
						BroadcastReceiverBuilder.CHARACTERISTIC_UUID);
				Integer viewId = views.get(charUuid);
				if (viewId != null) {
					byte[] value = (byte[]) intent.getExtras().get(
							BroadcastReceiverBuilder.CHARACTERISTIC_VALUE);
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
		Log.i(TAG, EXTRA_BLE_DEVICE + " was selected, now resuming services...");
		registerReceiver(metaWearUpdateReceiver,
				MetaWearBLEService.getMetaWearIntentFilter());
		registerReceiver(bluetoothLeReceiver,
				MetaWearBLEService.getBLEIntentFilter());
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
		try {
			unbindService(mwServiceConnection);
			mwService = null;
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't registered, no MetaWear device...");
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		try {
			mwService.disconnect();
		} catch (NullPointerException e) {
			Log.i(TAG, "No service to kill...");
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
