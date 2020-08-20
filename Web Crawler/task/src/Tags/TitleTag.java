package Tags;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleTag implements Tag {
    protected URLConnection urlConnection;

    public TitleTag(URL url) {
        try {
            this.urlConnection = url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.urlConnection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
    }

    @Override
    public List<String> getAllTags() {
        String ans = "NOT_FOUND";
        ArrayList<String> list = new ArrayList<>();
        try {
            String type = urlConnection.getContentType();
            if (type != null && type.contains("text/html")) {
                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                Scanner sc = new Scanner(inputStream,"UTF-8");
                while (sc.hasNext()) {
                    String token = sc.nextLine();
                    Pattern start = Pattern.compile("<\\s*title\\s*>");
                    Matcher startMatcher = start.matcher(token);

                    Pattern end = Pattern.compile("<\\s*/\\s*title\\s*>");

                    if (startMatcher.find()) {
                        String initial = token.substring(startMatcher.end());
                        Matcher endMatcher = end.matcher(initial);
                        if (endMatcher.find()) {
                            ans = initial.substring(0,endMatcher.start());
                            list.add(ans);
                            break;
                        }
                    }
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return list;
    }
}
