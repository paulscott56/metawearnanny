package za.co.paulscott.metawearnanny;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ListView;

public class BLEScannerActivity extends ListActivity {
	private BluetoothAdapter mBluetoothAdapter;
	private int REQUEST_ENABLE_BT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blescanner);

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		mHandler = new Handler();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Ensures Bluetooth is enabled on the device. If Bluetooth is not
		// currently enabled,
		// fire an intent to display a dialog asking the user to grant
		// permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}

		// Initializes list view adapter.
		bleListAdapter = new BLEDeviceListAdapter(BLEScannerActivity.this,
				R.id.LinearLayout1, getLayoutInflater());
		setListAdapter(bleListAdapter);
		scanLeDevice(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
		bleListAdapter.clear();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final BluetoothDevice device = (BluetoothDevice) bleListAdapter
				.getItem(position);
		if (device == null)
			return;
		
		if (mScanning) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			mScanning = false;
		}

		Bundle bundle = new Bundle();
		bundle.putParcelable(MainActivity.EXTRA_BLE_DEVICE, device);
		Intent returnIntent = new Intent();
		returnIntent.putExtras(bundle);
		setResult(RESULT_OK,returnIntent);
		finish();
		
	}

	private static final long SCAN_PERIOD = 10000;
	private Handler mHandler;
	private boolean mScanning;
	private BLEDeviceListAdapter bleListAdapter;

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					bleListAdapter.add(device);
					bleListAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
				}
			}, SCAN_PERIOD);

			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}

}
