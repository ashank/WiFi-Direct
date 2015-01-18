/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.udacity.hackathon.ui;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.hackathon.R;
import com.udacity.hackathon.util.PrefUtils;

import java.util.ArrayList;
import java.util.List;

public class DeviceListFragment extends ListFragment {  // callback of requestPeers

    private static final String TAG = DeviceListFragment.class.getSimpleName();

    WiFiDirectApplication mApp = null;

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.peer_devices_row, peers));
        mApp = (WiFiDirectApplication) getActivity().getApplication();
        onPeersAvailable(mApp.mPeers);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        return mContentView;
    }

    /**
     * @return this device
     */
    public WifiP2pDevice getDevice() {
        return device;
    }

    /**
     * Initiate a connection with the peer.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        ((DeviceActionListener) getActivity()).showDetails(device);
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.peer_devices_row, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(PrefUtils.getString(getActivity().getApplicationContext(), "name"));
                }
                if (bottom != null) {
                    bottom.setText(ConnectionService.getDeviceStatus(device.status));
                }
                WiFiDirectApplication.PTPLog.d(TAG, "WiFiPeerListAdapter : getView : " + device.deviceName);
            }
            return v;
        }
    }

    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */
    public void updateThisDevice(WifiP2pDevice device) {
        TextView nameview = (TextView) mContentView.findViewById(R.id.my_name);
        TextView statusview = (TextView) mContentView.findViewById(R.id.my_status);
        if (device != null) {
            WiFiDirectApplication.PTPLog.d(TAG, "updateThisDevice: " + device.deviceName + " = " + ConnectionService.getDeviceStatus(device.status));
            this.device = device;
            //nameview.setText(device.deviceName);
            nameview.setText(PrefUtils.getString(getActivity().getApplicationContext(), "name"));
            statusview.setText(ConnectionService.getDeviceStatus(device.status));
        } else if (this.device != null) {
            nameview.setText(PrefUtils.getString(getActivity().getApplicationContext(), "name"));
            statusview.setText("와이파이 다이렉트롤 켜주세요.");
        }
    }

    /**
     * the callback defined in PeerListListener to get the async result
     * from WifiP2pManager.requestPeers(channel, PeerListListener);
     */
    public void onPeersAvailable(List<WifiP2pDevice> peerList) {   // the callback to collect peer list after discover.
        if (progressDialog != null && progressDialog.isShowing()) {  // dismiss progressbar first.
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList);
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            WiFiDirectApplication.PTPLog.d(WiFiDirectActivity.TAG, "onPeersAvailable : No devices found");
            return;
        }
    }

    public void clearPeers() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                peers.clear();
                ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
                Toast.makeText(getActivity(), "연결이 끊겼습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
            }
        });

    }


    public void onInitiateDiscovery() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "취소는 back 버튼 클릭 ", "연결 대상을 찾고 있습니다.", true,
                true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                    }
                });
    }

    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config);

        void disconnect();
    }

}