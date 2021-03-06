/*
 * Copyright (c) 2013, Psiphon Inc.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package ca.psiphon.ploggy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

/**
 * User interface which displays self status details and
 * allows user to set message.
 *
 * This class subscribes to status events to update data
 * while in the foreground (e.g., location data updated
 * the the engine).
 */
public class FragmentSelfStatusDetails extends Fragment {

    private static final String LOG_TAG = "Self Status Details";

    private Fragment mFragmentComposeMessage;
    private ScrollView mScrollView;
    private ImageView mAvatarImage;
    private TextView mNicknameText;
    private TextView mFingerprintText;
    private ListView mMessagesList;
    private MessageAdapter mMessageAdapter;
    private TextView mLocationLabel;
    private TextView mLocationStreetAddressLabel;
    private TextView mLocationStreetAddressText;
    private TextView mLocationCoordinatesLabel;
    private TextView mLocationCoordinatesText;
    private TextView mLocationPrecisionLabel;
    private TextView mLocationPrecisionText;
    private TextView mLocationTimestampLabel;
    private TextView mLocationTimestampText;
    Utils.FixedDelayExecutor mRefreshUIExecutor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.self_status_details, container, false);

        mFragmentComposeMessage = new FragmentComposeMessage();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_self_status_details_compose_message, mFragmentComposeMessage).commit();

        mScrollView = (ScrollView)view.findViewById(R.id.self_status_details_scroll_view);
        mAvatarImage = (ImageView)view.findViewById(R.id.self_status_details_avatar_image);
        mNicknameText = (TextView)view.findViewById(R.id.self_status_details_nickname_text);
        mFingerprintText = (TextView)view.findViewById(R.id.self_status_details_fingerprint_text);
        mMessagesList = (ListView)view.findViewById(R.id.self_status_details_messages_list);
        mLocationLabel = (TextView)view.findViewById(R.id.self_status_details_location_label);
        mLocationStreetAddressLabel = (TextView)view.findViewById(R.id.self_status_details_location_street_address_label);
        mLocationStreetAddressText = (TextView)view.findViewById(R.id.self_status_details_location_street_address_text);
        mLocationCoordinatesLabel = (TextView)view.findViewById(R.id.self_status_details_location_coordinates_label);
        mLocationCoordinatesText = (TextView)view.findViewById(R.id.self_status_details_location_coordinates_text);
        mLocationPrecisionLabel = (TextView)view.findViewById(R.id.self_status_details_location_precision_label);
        mLocationPrecisionText = (TextView)view.findViewById(R.id.self_status_details_location_precision_text);
        mLocationTimestampLabel = (TextView)view.findViewById(R.id.self_status_details_location_timestamp_label);
        mLocationTimestampText = (TextView)view.findViewById(R.id.self_status_details_location_timestamp_text);

        // TODO: use header/footer of listview instead of hack embedding of listview in scrollview
        // from: http://stackoverflow.com/questions/4490821/scrollview-inside-scrollview/11554823#11554823
        mScrollView.setOnTouchListener(
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    mMessagesList.requestDisallowInterceptTouchEvent(false);
                    return false;
                }
            });
        mMessagesList.setOnTouchListener(
            new View.OnTouchListener() {
                private float downX, downY;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    // We want to capture vertical scrolling motions for use by
                    // the messages list, but we don't want to capture horizontal
                    // swiping that should be used to switch tabs. So we're going
                    // to decide based on whether the move looks more X-ish or Y-ish.
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        downX = event.getX();
                        downY = event.getY();

                        // Make sure the parent is allowed to intercept.
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        float deltaX = downX - event.getX();
                        float deltaY = downY - event.getY();
                        if (Math.abs(deltaY) > Math.abs(deltaX)) {
                            // Looks like a Y-ish scroll attempt. Disallow parent from intercepting.
                            view.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }

                    return false;
                }
            });

        try {
            mMessageAdapter = new MessageAdapter(getActivity(), MessageAdapter.Mode.SELF_MESSAGES);
            mMessagesList.setAdapter(mMessageAdapter);
        } catch (Utils.ApplicationError e) {
            Log.addEntry(LOG_TAG, "failed to load self messages");
        }

        show(view);

        // Refresh the message list every 5 seconds. This updates "time ago" displays.
        // TODO: event driven redrawing?
        mRefreshUIExecutor = new Utils.FixedDelayExecutor(new Runnable() {@Override public void run() {show();}}, 5000);

        Events.register(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRefreshUIExecutor.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRefreshUIExecutor.stop();
    }

    @Override
    public void onDestroyView() {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.remove(mFragmentComposeMessage).commitAllowingStateLoss();
        Events.unregister(this);
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Note: require explicit result routing for nested fragment
        if (mFragmentComposeMessage != null) {
            mFragmentComposeMessage.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Subscribe
    public void onUpdatedSelf(Events.UpdatedSelf updatedSelf) {
        show();
    }

    @Subscribe
    public void onUpdatedSelfStatus(Events.UpdatedSelfStatus updatedSelfStatus) {
        show();
    }

    private void show() {
        View view = getView();
        if (view == null) {
            return;
        }
        show(view);
    }

    private void show(View view) {
        try {
            Data data = Data.getInstance();
            Data.Self self = data.getSelf();
            Data.Status selfStatus = data.getSelfStatus();
            // Not using selfStatus.mLocation as it's not updated when location sharing is off
            // TODO: cleaner API
            Data.Location selfLocation = data.getCurrentSelfLocation();

            Robohash.setRobohashImage(getActivity(), mAvatarImage, true, self.mPublicIdentity);
            mNicknameText.setText(self.mPublicIdentity.mNickname);
            mFingerprintText.setText(Utils.formatFingerprint(self.mPublicIdentity.getFingerprint()));

            // Note: always show message section label and content edit
            int messageVisibility = (selfStatus.mMessages.size() > 0) ? View.VISIBLE : View.GONE;
            mMessagesList.setVisibility(messageVisibility);
            if (mMessageAdapter != null) {
                mMessageAdapter.updateMessages();
            }
            int locationVisibility = (selfLocation.mTimestamp != null) ? View.VISIBLE : View.GONE;
            mLocationLabel.setVisibility(locationVisibility);
            mLocationStreetAddressLabel.setVisibility(locationVisibility);
            mLocationStreetAddressText.setVisibility(locationVisibility);
            mLocationCoordinatesLabel.setVisibility(locationVisibility);
            mLocationCoordinatesText.setVisibility(locationVisibility);
            mLocationPrecisionLabel.setVisibility(locationVisibility);
            mLocationPrecisionText.setVisibility(locationVisibility);
            mLocationTimestampLabel.setVisibility(locationVisibility);
            mLocationTimestampText.setVisibility(locationVisibility);
            if (selfLocation.mTimestamp != null) {
                if (selfLocation.mStreetAddress.length() > 0) {
                    mLocationStreetAddressText.setText(selfLocation.mStreetAddress);
                } else {
                    mLocationStreetAddressText.setText(R.string.prompt_no_street_address_reported);
                }
                mLocationCoordinatesText.setText(
                        getString(
                                R.string.format_status_details_coordinates,
                                selfLocation.mLatitude,
                                selfLocation.mLongitude));
                mLocationPrecisionText.setText(
                        getString(
                                R.string.format_status_details_precision,
                                selfLocation.mPrecision));
                mLocationTimestampText.setText(Utils.DateFormatter.formatRelativeDatetime(getActivity(), selfLocation.mTimestamp, true));
            }
        } catch (Utils.ApplicationError e) {
            // TODO: hide identity/message views?
            Log.addEntry(LOG_TAG, "failed to display self status details");
        }
    }
}
