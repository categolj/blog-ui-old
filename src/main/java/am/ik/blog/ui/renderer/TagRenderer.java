package am.ik.blog.ui.renderer;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TagRenderer {

    public String render(List<String> tags) {
        UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentContextPath();
        return tags.stream()
                .map(tag -> "<span class=\"label label-default\"><a class=\"type-neutral-11\" href=\"" + builder.replacePath("/tags/{tag}/entries").buildAndExpand(tag) + "\">" + tag + "</a></span>")
                .collect(Collectors.joining(" "));
    }

    public String renderOne(String tag) {
        UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentContextPath();
        return "<span class=\"label label-default\"><a class=\"type-neutral-11\" href=\"" + builder.replacePath("/tags/{tag}/entries").buildAndExpand(tag) + "\">" + tag + "</a></span>";
    }
}
