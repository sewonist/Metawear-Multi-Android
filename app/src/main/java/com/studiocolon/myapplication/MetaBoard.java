
package com.studiocolon.myapplication;

        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothManager;
        import android.content.Context;
        import android.util.Log;
        import android.widget.Toast;

        import com.mbientlab.metawear.AsyncOperation;
        import com.mbientlab.metawear.Message;
        import com.mbientlab.metawear.MetaWearBleService;
        import com.mbientlab.metawear.MetaWearBoard;
        import com.mbientlab.metawear.RouteManager;
        import com.mbientlab.metawear.UnsupportedModuleException;
        import com.mbientlab.metawear.data.CartesianFloat;
        import com.mbientlab.metawear.data.CartesianShort;
        import com.mbientlab.metawear.module.Bmi160Accelerometer;
        import com.mbientlab.metawear.module.Bmi160Gyro;
        import com.mbientlab.metawear.module.Bmm150Magnetometer;
        import com.mbientlab.metawear.module.Debug;
        import com.mbientlab.metawear.module.Logging;

/**
 * Created by studiocolon on 2016-10-11.
 */

public class MetaBoard {
    private Context mContext;
    private MetaWearBleService.LocalBinder binder;
    private MetaWearBoard mwBoard;
    private String address;
    private Callback callback;

    private boolean boardSetup = false;
    private boolean accelSetup = false;
    private boolean gyroSetup = false;
    private boolean magnetoSetup= false;


    public MetaBoard(Context mContext, MetaWearBleService.LocalBinder binder, String address, Callback callback) {
        this.mContext = mContext;
        this.binder = binder;
        this.address = address;
        this.callback = callback;
    }

    // connect 함수는 반드시 한번만 호출됨을 보장해야 함.
    public void connect() {

        Log.d(MainActivity.TAG, "Try to connect. "+address);

        if(boardSetup == false) {
            final BluetoothManager btManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(address);

            binder.executeOnUiThread();
            //binder.clearCachedState(remoteDevice);
            mwBoard = binder.getMetaWearBoard(remoteDevice);
            mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
                @Override
                public void connected() {
                    Toast.makeText(mContext, "Connected: "+address, Toast.LENGTH_LONG).show();

                    Log.i(MainActivity.TAG, "Connected");
                    Log.i(MainActivity.TAG, "MetaBoot? " + mwBoard.inMetaBootMode());

                    mwBoard.readDeviceInformation().onComplete(new AsyncOperation.CompletionHandler<MetaWearBoard.DeviceInformation>() {
                        @Override
                        public void success(MetaWearBoard.DeviceInformation result) {
                            Log.i(MainActivity.TAG, "Device Information: " + result.toString());
                        }

                        @Override
                        public void failure(Throwable error) {
                            Log.e(MainActivity.TAG, "Error reading device information", error);
                        }
                    });
                    programSensor();

                    callback.OnConnect();
                }

                @Override
                public void disconnected() {
                    Toast.makeText(mContext, "Disconnected", Toast.LENGTH_SHORT).show();
                    Log.i(MainActivity.TAG, "Disconnected");
                    connect();
                }

                @Override
                public void failure(int status, final Throwable error) {
                    Toast.makeText(mContext, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(MainActivity.TAG, "Error connecting", error);
                    connect();
                }
            });
            // connect to board
            mwBoard.connect();
            boardSetup = true;
        } else {
            mwBoard.connect();
        }
    }

    // TODO: discnnect to board
    public void disconnect() {
    }

    public void readRssi() {
        mwBoard.readRssi().onComplete(new AsyncOperation.CompletionHandler<Integer>() {
            @Override
            public void success(final Integer result) {

            }

            @Override
            public void failure(Throwable error) {
                Log.e(MainActivity.TAG, "Error reading RSSI value", error);
            }
        });
    }

    public void readBattery() {
        mwBoard.readBatteryLevel().onComplete(new AsyncOperation.CompletionHandler<Byte>() {
            @Override
            public void success(final Byte result) {

            }

            @Override
            public void failure(Throwable error) {
                Log.e(MainActivity.TAG, "Error reading battery level", error);
            }
        });
    }
    // end of call from unity --------------------------------------------------



    public void programSensor() {
        programAccelero();
        //programGyro();
        //programMagneto();
    }

    public void programAccelero() {
        try {
            final Bmi160Accelerometer accelModule = mwBoard.getModule(Bmi160Accelerometer.class);
            if (!accelSetup) {
                accelModule.routeData().fromAxes().stream("accelSub")
                        .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("accelSub", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                final CartesianShort axisData = msg.getData(CartesianShort.class);

                                Log.i(MainActivity.TAG, address+" "+axisData.toString());
                            }
                        });

                        accelSetup = true;
                        accelModule.configureAxisSampling()
                                .setFullScaleRange(Bmi160Accelerometer.AccRange.AR_16G)
                                .setOutputDataRate(Bmi160Accelerometer.OutputDataRate.ODR_25_HZ)
                                .commit();
                        accelModule.enableAxisSampling();
                        accelModule.start();
                    }

                    @Override
                    public void failure(Throwable error) {
                        Log.e(MainActivity.TAG, "Error committing route", error);
                        accelSetup = false;
                    }
                });
            } else {
                accelModule.enableAxisSampling();
                accelModule.start();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(mContext, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void programGyro() {
        try {
            final Bmi160Gyro gyroModule = mwBoard.getModule(Bmi160Gyro.class);

            if (!gyroSetup) {
                gyroModule.routeData().fromAxes().stream("gyro_stream")
                        .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("gyro_stream", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                final CartesianFloat spinData = msg.getData(CartesianFloat.class);

                                Log.i(MainActivity.TAG, spinData.toString());
                            }
                        });
                        gyroModule.configure().setOutputDataRate(Bmi160Gyro.OutputDataRate.ODR_25_HZ)
                                .setFullScaleRange(Bmi160Gyro.FullScaleRange.FSR_2000)
                                .commit();
                        gyroModule.start();
                    }
                });
                gyroSetup= true;
            } else {
                gyroModule.configure().setOutputDataRate(Bmi160Gyro.OutputDataRate.ODR_25_HZ)
                        .setFullScaleRange(Bmi160Gyro.FullScaleRange.FSR_125)
                        .commit();
                gyroModule.start();
            }

        } catch (UnsupportedModuleException e) {
            Toast.makeText(mContext, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void programMagneto() {
        try {
            final Bmm150Magnetometer magModule = mwBoard.getModule(Bmm150Magnetometer.class);
            final Logging loggingModule= mwBoard.getModule(Logging.class);
            if (!magnetoSetup) {
                //Set to low power mode
                magModule.setPowerPreset(Bmm150Magnetometer.PowerPreset.LOW_POWER);
                magModule.enableBFieldSampling();

                //Stream rotation data around the XYZ axes from the gyro sensor
                magModule.routeData().fromBField().stream("mag_stream").commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.subscribe("mag_stream", new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message msg) {
                                        final CartesianFloat bField = msg.getData(CartesianFloat.class);

                                        Log.i(MainActivity.TAG, bField.toString());
                                    }
                                });

                                magModule.start();
                            }
                        });
                magnetoSetup = true;
            }  else {
                loggingModule.startLogging();
                magModule.start();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(mContext, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
