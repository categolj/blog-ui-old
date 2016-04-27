package am.ik.blog.ui.renderer;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
public class CategoriesRenderer {

    public String render(List<String> categories) {
        UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentContextPath();
        List<String> ret = new ArrayList<>(categories.size());
        List<String> buf = new ArrayList<>(categories.size());
        for (String category : categories) {
            buf.add(category);
            builder.replacePath("/categories/{categories}/entries");
            ret.add("<a href=\"" + builder.buildAndExpand(String.join(",", buf)) + "\">" + category + "</a>");
        }
        return String.join("::", ret);
    }
}
