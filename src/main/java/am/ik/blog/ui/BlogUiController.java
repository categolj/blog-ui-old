package am.ik.blog.ui;

import am.ik.marked4j.Marked;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
@Slf4j
public class BlogUiController {

    @Autowired
    BlogClient blogClient;
    @Autowired
    Marked marked;
    @Value("${blog.api.url:http://localhost:8080}")
    String apiUrl;

    @RequestMapping(value = {"/", "/entries"})
    String home(Model model, @PageableDefault(size = 10) Pageable pageable) {
        Page entries = blogClient.findAll(pageable);
        model.addAttribute("page", entries);
        return "index";
    }

    @RequestMapping(value = {"/", "/entries"}, params = "q")
    String search(Model model, @RequestParam("q") String query, @PageableDefault(size = 10) Pageable pageable) {
        Page entries = blogClient.findByQuery(query, pageable);
        model.addAttribute("page", entries);
        return "index";
    }

    @RequestMapping(path = "/entries/{entryId}")
    String byId(Model model, @PathVariable("entryId") Long entryId) {
        Entry entry = blogClient.findById(entryId);
        model.addAttribute("entry", entry);
        return "entry";
    }

    @RequestMapping(path = "/entries/{entryId}", params = "partial")
    @ResponseBody
    String partialById(@PathVariable("entryId") Long entryId) {
        Entry entry = blogClient.findById(entryId);
        return marked.marked(entry.getContent());
    }

    @RequestMapping(path = "/tags/{tag}/entries")
    String byTag(Model model, @PathVariable("tag") String tag, @PageableDefault(size = 10) Pageable pageable) {
        Page entries = blogClient.findByTag(tag, pageable);
        model.addAttribute("page", entries);
        return "index";
    }

    @RequestMapping(path = "/categories/{categories}/entries")
    String byCategories(Model model, @PathVariable("categories") String categories, @PageableDefault(size = 10) Pageable pageable) {
        Page entries = blogClient.findByCategories(categories, pageable);
        model.addAttribute("page", entries);
        return "index";
    }

    @RequestMapping(path = "/users/{name}/entries")
    String byCreatedBy(Model model, @PathVariable("name") String name, @PageableDefault(size = 10) Pageable pageable) {
        Page entries = blogClient.findByCreatedBy(name, pageable);
        model.addAttribute("page", entries);
        return "index";
    }

    @RequestMapping(path = "/users/{name}/entries", params = "updated")
    String byUpdatedBy(Model model, @PathVariable("name") String name, @PageableDefault(size = 10) Pageable pageable) {
        Page entries = blogClient.findByUpdatedBy(name, pageable);
        model.addAttribute("page", entries);
        return "index";
    }

    @RequestMapping(path = "/tags")
    String tags(Model model) {
        List<String> tags = blogClient.findTags();
        model.addAttribute("tags", tags);
        return "tags";
    }

    @RequestMapping(path = "/categories")
    String categories(Model model) {
        List<List<String>> categories = blogClient.findCategories();
        model.addAttribute("categories", categories);
        return "categories";
    }

    @ExceptionHandler(ResourceAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    String handleResourceAccessException(ResourceAccessException e) {
        log.error("Cannot access " + apiUrl, e);
        return "error/api-server-down";
    }

    @ExceptionHandler(HttpServerErrorException.class)
    String handleHttpServerErrorException(HttpServerErrorException e) {
        log.error("API Server Error", e);
        return "error/api-server-error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    String handleException(Exception e) {
        log.error("Unexpected Error", e);
        return "error/unexpected-error";
    }


    @Component
    public static class BlogClient {
        @Autowired
        RestTemplate restTemplate;
        @Value("${blog.api.url:http://localhost:8080}")
        String apiUrl;

        final ParameterizedTypeReference<Page> typeReference = new ParameterizedTypeReference<Page>() {
        };

        public Entry fallbackEntry(Long entryId) {
            return Entry.builder().entryId(entryId)
                    .frontMatter(FrontMatter.builder()
                            .title("Service is unavailable now x( !")
                            .categories(Collections.emptyList())
                            .tags(Collections.emptyList())
                            .build())
                    .content("Wait a minute...")
                    .created(Author.builder().name("system").date(OffsetDateTime.now()).build())
                    .updated(Author.builder().name("system").date(OffsetDateTime.now()).build())
                    .build();
        }

        public Page fallbackPage(Pageable pageable) {
            return Page.builder()
                    .content(Collections.singletonList(fallbackEntry(0L)))
                    .first(true)
                    .last(true)
                    .number(1)
                    .numberOfElements(1)
                    .totalElements(1)
                    .totalPages(1)
                    .build();
        }

        public Page fallbackPage(String dummy, Pageable pageable) {
            return Page.builder()
                    .content(Collections.singletonList(fallbackEntry(0L)))
                    .first(true)
                    .last(true)
                    .number(1)
                    .numberOfElements(1)
                    .totalElements(1)
                    .totalPages(1)
                    .build();
        }

        public List<String> fallbackTags() {
            return Arrays.asList("Service", "is", "unavailable", "now");
        }

        public List<List<String>> fallbackCategories() {
            return Arrays.asList(fallbackTags());
        }

        @HystrixCommand(fallbackMethod = "fallbackPage")
        public Page findAll(Pageable pageable) {
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .pathSegment("api", "entries")
                    .queryParam("page", pageable.getPageNumber())
                    .queryParam("size", pageable.getPageSize())
                    .queryParam("excludeContent", true)
                    .build();
            return restTemplate.exchange(uri.toUri(), HttpMethod.GET, HttpEntity.EMPTY, typeReference).getBody();
        }

        @HystrixCommand(fallbackMethod = "fallbackPage")
        public Page findByQuery(String query, Pageable pageable) {
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .pathSegment("api", "entries")
                    .queryParam("q", query)
                    .queryParam("page", pageable.getPageNumber())
                    .queryParam("size", pageable.getPageSize())
                    .queryParam("excludeContent", true)
                    .build();
            return restTemplate.exchange(uri.toUri(), HttpMethod.GET, HttpEntity.EMPTY, typeReference).getBody();
        }

        @HystrixCommand(fallbackMethod = "fallbackEntry")
        public Entry findById(Long entryId) {
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .pathSegment("api", "entries", entryId.toString())
                    .build();
            return restTemplate.exchange(uri.toUri(), HttpMethod.GET, HttpEntity.EMPTY, Entry.class).getBody();
        }

        @HystrixCommand(fallbackMethod = "fallbackPage")
        public Page findByTag(String tag, Pageable pageable) {
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .pathSegment("api", "tags", tag, "entries")
                    .queryParam("page", pageable.getPageNumber())
                    .queryParam("size", pageable.getPageSize())
                    .queryParam("excludeContent", true)
                    .build();
            return restTemplate.exchange(uri.toUri(), HttpMethod.GET, HttpEntity.EMPTY, typeReference).getBody();
        }

        @HystrixCommand(fallbackMethod = "fallbackPage")
        public Page findByCategories(String categories, Pageable pageable) {
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .pathSegment("api", "categories", categories, "entries")
                    .queryParam("page", pageable.getPageNumber())
                    .queryParam("size", pageable.getPageSize())
                    .queryParam("excludeContent", true)
                    .build();
            return restTemplate.exchange(uri.toUri(), HttpMethod.GET, HttpEntity.EMPTY, typeReference).getBody();
        }

        @HystrixCommand(fallbackMethod = "fallbackPage")
        public Page findByCreatedBy(String name, Pageable pageable) {
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .pathSegment("api", "users", name, "entries")
                    .queryParam("page", pageable.getPageNumber())
                    .queryParam("size", pageable.getPageSize())
                    .queryParam("excludeContent", true)
                    .build();
            return restTemplate.exchange(uri.toUri(), HttpMethod.GET, HttpEntity.EMPTY, typeReference).getBody();
        }

        @HystrixCommand(fallbackMethod = "fallbackPage")
        public Page findByUpdatedBy(String name, Pageable pageable) {
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .pathSegment("api", "users", name, "entries")
                    .queryParam("updated")
                    .queryParam("page", pageable.getPageNumber())
                    .queryParam("size", pageable.getPageSize())
                    .queryParam("excludeContent", true)
                    .build();
            return restTemplate.exchange(uri.toUri(), HttpMethod.GET, HttpEntity.EMPTY, typeReference).getBody();
        }

        @HystrixCommand(fallbackMethod = "fallbackTags")
        public List<String> findTags() {
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .pathSegment("api", "tags")
                    .build();
            return restTemplate.exchange(uri.toUri(), HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<String>>() {
            }).getBody();
        }

        @HystrixCommand(fallbackMethod = "fallbackCategories")
        public List<List<String>> findCategories() {
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .pathSegment("api", "categories")
                    .build();
            return restTemplate.exchange(uri.toUri(), HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<List<String>>>() {
            }).getBody();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Page {
        private List<Entry> content;
        private String sort;
        private long totalPages;
        private long totalElements;
        private boolean first;
        private boolean last;
        private long numberOfElements;
        private int size;
        private int number;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Author implements Serializable {
        private String name;
        private OffsetDateTime date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry implements Serializable {
        private Long entryId;
        private String content;
        private Author created;
        private Author updated;
        private FrontMatter frontMatter;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrontMatter implements Serializable {
        private String title;
        private List<String> tags;
        private List<String> categories;
    }
}
