package kr.co.example.rssreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

class MainActivity : AppCompatActivity() {

    // 클래스 레벨에서 디스패처 생성
    private val netDispatcher = newSingleThreadContext(name = "ServiceCall")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 특정 디스패처로 코루틴 실행
        GlobalScope.launch(netDispatcher) {
            // TODO Call coroutine here
        }
    }
}