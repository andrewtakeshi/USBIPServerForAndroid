package org.cgutman.usbip.config;

import org.cgutman.usbip.service.UsbIpService;
import org.cgutman.usbipserverforandroid.R;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

public class UsbIpConfig extends ComponentActivity {
	private Button serviceButton;
	private TextView serviceStatus;
	private TextView serviceReadyText;
	
	private boolean running;

	private ActivityResultLauncher<String> requestPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
				// We don't actually care if the permission is granted or not. We will launch the service anyway.
				launchService();
			});
	
	private void updateStatus() {
		if (running) {
			serviceButton.setText("Stop Service");
			serviceStatus.setText("USB/IP Service Running");
			serviceReadyText.setText(R.string.ready_text);
		}
		else {
			serviceButton.setText("Start Service");
			serviceStatus.setText("USB/IP Service Stopped");
			serviceReadyText.setText("");
		}
	}
	private void launchService() {
		Intent intent = new Intent(this, UsbIpService.class);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(intent);
		} else {
			startService(intent);
		}
	}

	
	private boolean isMyServiceRunning(Class<?> serviceClass) {
		return UsbIpService.isRunning;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_usbip_config);

		// Handle USB_DEVICE_ATTACHED intent — opening the device here
		// grants implicit permission that the service can use later
		handleUsbIntent(getIntent());

		// Apply WindowInsets for edge-to-edge on API 35+
		if (Build.VERSION.SDK_INT >= 35) {
			View rootLayout = findViewById(R.id.rootLayout);
			androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
				androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
				v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
				return androidx.core.view.WindowInsetsCompat.CONSUMED;
			});
		}

		serviceButton = findViewById(R.id.serviceButton);
		serviceStatus = findViewById(R.id.serviceStatus);
		serviceReadyText = findViewById(R.id.serviceReadyText);
		
		running = isMyServiceRunning(UsbIpService.class);
		
		updateStatus();
		
		serviceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (running) {
					stopService(new Intent(UsbIpConfig.this, UsbIpService.class));
				}
				else {
					if (ContextCompat.checkSelfPermission(UsbIpConfig.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
						launchService();
					} else {
						requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
					}
				}
				
				running = !running;
				updateStatus();
			}
		});
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleUsbIntent(intent);
	}

	private void handleUsbIntent(Intent intent) {
		if (intent != null && UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
			UsbDevice dev;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
			} else {
				dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			}
			if (dev != null) {
				UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
				System.out.printf("USB device attached via intent: vid=%04x pid=%04x hasPermission=%b\n",
						dev.getVendorId(), dev.getProductId(), usbManager.hasPermission(dev));
			}
		}
	}
}
