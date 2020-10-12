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

    // immutable list
    val feeds = listOf(
        "https://www.npr.org/rss/rss.php?id=1001",
        "http://rss.cnn.com/rss/cnn_topstories.rss",
        "http://feeds.foxnews.com/foxnews/politics?format=xml"
    )
    // 크기가 2인 스레드 풀을 만들고 IO로 이름을 변경
    // asyncFetchHeadlines()는 서버에서 정보를 가져올 뿐 아니라 파싱도 하기 때문에 풀의 크기를 늘림
    // XML을 파싱하는 오버 헤드는 단일 스레드를 사용하는 경우 성능에 영향
    // 때로는 다른 스레드의 파싱이 완료될 때까지 한 피드로부터 정보를 가져오는 것이 지연될 수 있음
    val dispatcher = newFixedThreadPoolContext(2, "IO")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 미리 정의된 디스패처를 갖는 비동기 함수
        // launch()를 포함하고 결과인 Job을 반환하는 함수
        // Job을 반환해서 호출자가 취소할 수 있음
        asyncLoadNews2()
    }

    private fun asyncLoadNews2() = GlobalScope.launch {
        // 동시에 여러 피드에 요청을 보내야 함
        // 목록에서 각 피드 당 하나의 디퍼드를 생성함
        // 따라서 대기하는 모든 디퍼드를 추적할 수 있는 목록 생성
        val requests = mutableListOf<Deferred<List<String>>>()

        // 각 피드별로 가져온 요소를 피드 목록에 추가
        feeds.mapTo(requests) {
            asyncFetchHeadlines(it, dispatcher)
        }

        // 각 코드가 완료될 때까지 대기하는 코드 추가
        requests.forEach {
            it.await()
        }

        // 각 디퍼드의 내용을 담은 변수
        val headlines = requests.flatMap {
            it.getCompleted()
        }

        // 헤드라인의 개수와 가져온 피드 개수 UI에 표시
        GlobalScope.launch(Dispatchers.Main) {
            newsCount.text = "Found ${ headlines.size } News in ${ requests.size } feeds"
        }
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

    private fun asyncFetchHeadlines(feed: String, dispatcher: CoroutineDispatcher) = GlobalScope.async(dispatcher) {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse(feed)  // feed 인수를 URL로 사용
        val news = xml.getElementsByTagName("channel").item(0)

        (0 until news.childNodes.length)
            .asSequence()
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map { it as Element }
            .filter { "item" == it.tagName }
            .map { it.getElementsByTagName("title").item(0).textContent }
            .toList()
    }
}