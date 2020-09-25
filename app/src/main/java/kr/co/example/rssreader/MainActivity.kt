package kr.co.example.rssreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
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