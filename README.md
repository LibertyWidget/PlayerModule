# PlayerModule
视频播放器

个人使用项目如有喜欢请下载

原始版本地址

https://github.com/lipangit/JiaoZiVideoPlayer
      public class MainActivity extends AppCompatActivity {

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.activity_main);
          UniversalPlayerView baseUniversalPlayerView = findViewById(R.id.playerView);
          String url = "http://h1.aaccy.com/ckplayer/pptv/sj.1.m3u8?ts=1547346666&key=bc74200750474ea4e41502f18108bed4&id=xtpiRfZWsByNmM1EHf8KVg";
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
