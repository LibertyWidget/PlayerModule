# PlayerModule
视频播放器

个人使用项目如有喜欢请下载

原始版本地址

https://github.com/lipangit/JiaoZiVideoPlayer

            
AndroidManifest.xml
           
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:hardwareAccelerated="true"
            android:screenOrientation="portrait"
            
MainActivity.java

      public class MainActivity extends AppCompatActivity {
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);
                UniversalPlayerView baseUniversalPlayerView = findViewById(R.id.playerView);
                String url = "播放地址";
                PlayManager.$().init(baseUniversalPlayerView);
                PlayManager.$().onSettingPlay(url, "title");
            }

            @Override
            public void finish() {
                PlayManager.$().finish();
                super.finish();
            }

            @Override
            public void onBackPressed() {
                PlayManager.$().onBackPressed();
                super.onBackPressed();
            }

            @Override
            protected void onResume() {
                PlayManager.$().onResume();
                super.onResume();
            }

            @Override
            protected void onPause() {
                PlayManager.$().onPause();
                super.onPause();
            }
      }
activity_main.xml

      <com.util.player.UniversalPlayerView
        android:id="@+id/playerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
