package kr.co.example.rssreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {

    // 클래스 레벨에서 디스패처 생성
    private val netDispatcher = newSingleThreadContext(name = "ServiceCall")
    private val factory = DocumentBuilderFactory.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 특정 디스패처로 코루틴 실행
        GlobalScope.launch(netDispatcher) {
            val headlines = fetchRssHeadlines()
//            newsCount.text = "Found ${ headlines.size } News" // CalledFromWrongThreadException Error
            /*
            * CalledFromWrongThreadException :
            * 코루틴의 내용은 백그라운드 스레드에서 실행 중이며
            * UI 업데이트는 UI 스레드에서 일어나야 함
            */

            // 안드로이드의 UI 코루틴 디스패처 사용
            GlobalScope.launch(Dispatchers.Main) {
                newsCount.text = "Found ${ headlines.size } News"
            }
        }

        // 비동기 호출자로 감싼 동기 함수
        // 비동기로 실행되는 코드라는 것을 명시적으로 나타내는 좋은 사례
        // loadNews() 호출이 많으면 유사한 블록이 코드에 많이 분산돼 가시성이 떨어짐
        GlobalScope.launch(netDispatcher) {
            loadNews()
        }

        // 미리 정의된 디스패처를 갖는 비동기 함수
        // launch()를 포함하고 결과인 Job을 반환하는 함수
        // Job을 반환해서 호출자가 취소할 수 있음
        asyncLoadNews()
    }

    // 미리 정의된 디스패처를 갖는 비동기 함수
    // 유연한 디스패처를 가지는 비동기 함수 : 단점 - 함수에 적절한 이름이 주어졌을 때만 명시적
    private fun asyncLoadNews(dispatcher: CoroutineDispatcher = netDispatcher) = GlobalScope.launch(dispatcher) {
        val headlines = fetchRssHeadlines()
        GlobalScope.launch(Dispatchers.Main) {
            newsCount.text = "Found ${ headlines.size } News"
        }
    }

    // 비동기 호출자로 감싼 동기 함수
    private fun loadNews() {
        val headlines = fetchRssHeadlines()
        GlobalScope.launch(Dispatchers.Main) {
            newsCount.text = "Found ${ headlines.size } News"
        }
    }

    // feed를 호출한 후 응답Response의 본문body을 읽고 헤드라인을 반환
    private fun fetchRssHeadlines(): List<String> {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse("https://www.npr.org/rss/rss.php?id=1001")  // 주소에서 내용 확인
        val news = xml.getElementsByTagName("channel").item(0)

        // 단순히 XML의 모든 요소들을 검사하면서 피드에 있는 각 기사article의 제목title을 제외한 모든 것을 필터링
        return (0 until news.childNodes.length)
            .asSequence()
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map { it as Element }
            .filter { "item" == it.tagName }
            .map { it.getElementsByTagName("title").item(0).textContent }
            .toList()
    }
}