package Tags;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkTag implements Tag {

    protected String htmlPage;
    protected URL url;

    public LinkTag(URL url, String htmlPage) {
        this.htmlPage = htmlPage;
        this.url = url;
    }

    @Override
    public List<String> getAllTags() {
        TreeMap<String, String> urls = new TreeMap<>();
        List<String> allUrls = new ArrayList<>();
        Pattern linkTag = Pattern.compile("<\\s*[aA].*[\" ]>");
        Matcher linkMatcher = linkTag.matcher(this.htmlPage);

        while (linkMatcher.find()) {
            String a = linkMatcher.group();

            Pattern href = Pattern.compile("\\s*href\\s*=\\s*\"[^\"]*\"",Pattern.CASE_INSENSITIVE);
            Matcher hrefMatcher = href.matcher(a);
            String link = "";
            if (hrefMatcher.find()) {
                link = hrefMatcher.group();
                Matcher value = Pattern.compile("\"[^\"]+\"").matcher(link);
                link = value.find() ? link.substring(value.start() + 1,value.end() - 1) : link;
            } else {
                link = a;
            }
            link = convertToCanonicalURL(link);
            allUrls.add(link);
        }

        return allUrls;
    }

    private String convertToCanonicalURL(String link) {
        String ans = "";

        try {
            URL url = new URL(this.url, link);
            ans = url.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return ans;
    }
}
