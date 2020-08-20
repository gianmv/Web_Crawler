package crawler;

import Tags.LinkTag;
import Tags.Tag;
import Tags.TitleTag;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class Spider {
    protected URL URL;
    final protected String NOT_FOUND = "NOT FOUND";
    protected String htmlPage;
    protected String type;
    protected String title;
    protected boolean isOK;

    protected Tag titleTag;
    protected Tag linkTag;

    public Spider() {
        this.htmlPage = "NO HTML CODE FOUND";
        this.type = this.NOT_FOUND;
        this.isOK = false;
    }

    public Spider(String URL) {
        setURL(URL);
    }

    private void getAllHtmlPage() {
        StringBuffer sb = new StringBuffer();
        try {
            URLConnection urlConnection = URL.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            if (this.type != null && this.type.contains("text/html")) {
                try (InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream())) {
                    Scanner sc = new Scanner(inputStream, "UTF-8");
                    while (sc.hasNext()) {
                        sb.append(sc.nextLine());
                        sb.append('\n');
                    }
                    this.htmlPage = sb.toString();
                }
                this.isOK = true;
            } else {
                this.isOK = false;
            }

        } catch (IOException ioException) {
            ioException.printStackTrace();
            this.isOK = false;
        }
    }

    public void setURL(String URL) {
        try {
            this.URL = new URL(URL);
            this.type = this.URL.openConnection().getContentType();
            if (this.type != null) {
                this.titleTag = new TitleTag(this.URL);
                String ans = this.titleTag.getAllTags().get(0);
                this.title = ans != null ? ans : this.NOT_FOUND;
                this.isOK = true;
            } else {
                this.isOK = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.isOK = false;
        } catch (IndexOutOfBoundsException e) {
            System.err.printf("No se encontró tag \"Title\" en %s\n",URL);
            this.isOK = false;
        }

        this.htmlPage = "";

    }

    public String getTitle() {
        return this.title != null ? this.title : this.titleTag.getAllTags().get(0);
    }

    public String getHtmlPage() {
        return this.htmlPage;
    }

    public Map<String, String> getAllLinks() {
        getAllHtmlPage();
        this.linkTag = new LinkTag(this.URL,this.htmlPage);
        List<String> onlyUrls = this.linkTag.getAllTags();
        Map<String, String> urls = new TreeMap<>();

        urls.put(this.URL.toString(), this.getTitle());
        for (String x : onlyUrls) {
            Spider spidy = new Spider(x);
            if (spidy.isOK) {
                String title = spidy.getTitle();
                urls.put(x, title);
            }
        }
        System.out.println(urls);
        return urls;
    }
}

