package io.github.raeperd.realworld.domain.article.comment;

import io.github.raeperd.realworld.domain.article.Article;
import io.github.raeperd.realworld.domain.article.ArticleRepository;
import io.github.raeperd.realworld.domain.article.title.ArticleTitle;
import io.github.raeperd.realworld.domain.user.UserContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static java.util.stream.Collectors.toList;

@Service
public class CommentService {

    private final ArticleRepository articleRepository;
    private final UserContextHolder userContextHolder;

    public CommentService(ArticleRepository articleRepository, UserContextHolder userContextHolder) {
        this.articleRepository = articleRepository;
        this.userContextHolder = userContextHolder;
    }

    @Transactional
    public Comment commentArticleBySlug(String slug, Comment comment) {
        return articleRepository.findFirstBySlug(ArticleTitle.of(slug).toSlug())
                .map(article -> article.addComment(comment))
                .orElseThrow(NoSuchElementException::new);
    }

    @Transactional(readOnly = true)
    public CommentView viewCommentFromCurrentUser(Comment comment) {
        return userContextHolder.getCurrentUser()
                .map(currentUser -> currentUser.viewProfile(comment.getAuthor()))
                .map(profile -> CommentView.of(comment, profile))
                .orElseThrow(IllegalStateException::new);
    }

    @Transactional(readOnly = true)
    public List<CommentView> viewAllCommentsBySlugFromCurrentUser(String slug) {
        return articleRepository.findFirstBySlug(ArticleTitle.of(slug).toSlug())
                .map(Article::getComments).orElseThrow(NoSuchElementException::new)
                .stream()
                .map(this::viewCommentFromCurrentUser)
                .collect(toList());
    }

    @Transactional
    public boolean deleteCommentInArticleById(String slug, long id) {
        final var currentUser = userContextHolder.getCurrentUser()
                .orElseThrow(IllegalStateException::new);
        return articleRepository.findFirstBySlug(ArticleTitle.of(slug).toSlug())
                .map(article -> article.deleteCommentByIdAndUser(id, currentUser))
                .orElseThrow(NoSuchElementException::new);
    }
}
