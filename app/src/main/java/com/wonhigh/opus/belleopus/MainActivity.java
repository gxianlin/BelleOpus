package com.wonhigh.opus.belleopus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.wonhigh.opus.opuslib.OpusService;
import com.wonhigh.opus.opuslib.OpusTrackInfo;
import com.wonhigh.opus.opuslib.model.AudioPlayList;
import com.wonhigh.opus.opuslib.model.AudioTime;

import java.util.List;
import java.util.Map;

import static com.wonhigh.opus.opuslib.OpusEvent.EVENT_PLAY_DURATION;
import static com.wonhigh.opus.opuslib.OpusEvent.EVENT_PLAY_PROGRESS_POSITION;
import static com.wonhigh.opus.opuslib.OpusEvent.EVENT_PLAY_TRACK_INFO;
import static com.wonhigh.opus.opuslib.OpusEvent.EVENT_RECORD_PROGRESS;
import static com.wonhigh.opus.opuslib.OpusEvent.EVENT_TYPE;
import static com.wonhigh.opus.opuslib.OpusEvent.PLAY_GET_AUDIO_TRACK_INFO;
import static com.wonhigh.opus.opuslib.OpusEvent.PLAY_PROGRESS_UPDATE;
import static com.wonhigh.opus.opuslib.OpusEvent.RECORD_FAILED;
import static com.wonhigh.opus.opuslib.OpusEvent.RECORD_FINISHED;
import static com.wonhigh.opus.opuslib.OpusEvent.RECORD_PROGRESS_UPDATE;
import static com.wonhigh.opus.opuslib.OpusEvent.RECORD_STARTED;
import static com.wonhigh.opus.opuslib.utils.Config.ACTION_OPUS_UI_RECEIVER;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    ListView mListView;
    Button btnStart;
    boolean isStart;

    private Toast mToast;


    private AudioPlayList trackList;
    ListViewAdapter adapter;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int type = bundle.getInt(EVENT_TYPE, 0);
            switch (type) {
                case RECORD_STARTED:
                    btnStart.setText("停止录音");
                    showTip("录音开始");
                    break;

                case RECORD_PROGRESS_UPDATE:
                    //获取录音时间
                    String time = bundle.getString(EVENT_RECORD_PROGRESS);
                    showTip(time);
                    Log.e("opus", "录音时间：" + time);
                    break;

                case RECORD_FINISHED:
                    btnStart.setText("开始录音");
                    showTip("录音结束");
                    break;


                case PLAY_PROGRESS_UPDATE:
                    long position = bundle.getLong(EVENT_PLAY_PROGRESS_POSITION);
                    long duration = bundle.getLong(EVENT_PLAY_DURATION);
                    AudioTime t = new AudioTime();
                    t.setTimeInSecond(position);

                    if (duration != 0) {
                        int progress = (int) (100 * position / duration);
                        showTip(t.getTime() + ",播放进度：" + progress + "%");
                    }
                    break;


                case PLAY_GET_AUDIO_TRACK_INFO:
                    List<Map<String, Object>> trackList = null;
                    if (bundle.getSerializable(EVENT_PLAY_TRACK_INFO) != null) {
                        trackList = ((AudioPlayList) (bundle.getSerializable(EVENT_PLAY_TRACK_INFO))).getList();
                    }

                    MainActivity.this.trackList.clear();
                    if (trackList == null) return;
                    for (Map<String, Object> map : trackList) {
                        //TODO this is a test
                        if (map.get(OpusTrackInfo.TITLE_IMG).equals(0)) {
                            map.put(OpusTrackInfo.TITLE_IMG, R.mipmap.ic_launcher);
                            MainActivity.this.trackList.add(map);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        trackList = new AudioPlayList();

        initView();
        initBroadcast();
    }

    /**
     * Toast弹窗
     *
     * @param str 显示内容
     */
    private void showTip(String str) {
        mToast.setText(str);
        mToast.show();
    }

    /**
     * 注册广播监听
     */
    private void initBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OPUS_UI_RECEIVER);
        registerReceiver(receiver, filter);
    }


    /**
     * 初始化界面UI控件
     */
    private void initView() {
        mListView = findViewById(R.id.listview);
        btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(this);
        adapter = new ListViewAdapter(this, trackList.getList());
        mListView.setAdapter(adapter);


        mListView.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {

        isStart = !isStart;
        OpusService.recordToggle(getApplicationContext(), "");
        if (!isStart) {
            //更新录音列表
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    /**
     * 列表点击事件
     *
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String fileName = trackList.getList().get(position).get(OpusTrackInfo.TITLE_ABS_PATH).toString();
        OpusService.play(getApplicationContext(), fileName);
    }
}
