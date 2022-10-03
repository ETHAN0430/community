package com.cyf.community.controller;

import com.cyf.community.entity.DiscussPost;
import com.cyf.community.entity.Page;
import com.cyf.community.service.ElasticsearchService;
import com.cyf.community.service.LikeService;
import com.cyf.community.service.UserService;
import com.cyf.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    // search?keyword=xxx
    @GetMapping("/search")
    public String search(String keyword, Page page, Model model) throws IOException {
        // 搜索帖子
        List<DiscussPost> searchResult = elasticsearchService.searchDiscussPost(keyword, page.getOffset(), page.getLimit());

        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();

                map.put("post", post);
                map.put("user", userService.findUserById(post.getUserId()));
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }

        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);

        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null || searchResult.size() == 0 ? 0 : searchResult.size());

        return "/site/search";
    }
}
