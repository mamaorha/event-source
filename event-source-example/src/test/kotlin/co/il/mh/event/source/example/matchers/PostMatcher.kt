package co.il.mh.event.source.example.matchers

import co.il.mh.event.source.example.data.Post
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsAnything

interface PostMatcher {
    fun <T> T.mustMatch(matcher: Matcher<T>) {
        MatcherAssert.assertThat(this, matcher)
    }

    fun postMatcher(
        withId: Matcher<String> = IsAnything(),
        withAuthorMail: Matcher<String> = IsAnything(),
        withTitle: Matcher<String> = IsAnything(),
        withContent: Matcher<String> = IsAnything(),
        withUpdateTime: Matcher<Long> = IsAnything(),
        withCreationTime: Matcher<Long> = IsAnything(),
        withDeleted: Matcher<Boolean> = IsAnything()
    ): Matcher<Post> {
        return object : TypeSafeMatcher<Post>() {
            override fun describeTo(description: Description) {
                description.appendText("a Post with properties: ")
                description.appendText("id ").appendDescriptionOf(withId)
                description.appendText(", authorMail ").appendDescriptionOf(withAuthorMail)
                description.appendText(", title ").appendDescriptionOf(withTitle)
                description.appendText(", content ").appendDescriptionOf(withContent)
                description.appendText(", updateTime ").appendDescriptionOf(withUpdateTime)
                description.appendText(", creationTime ").appendDescriptionOf(withCreationTime)
                description.appendText(", deleted ").appendDescriptionOf(withDeleted)
            }

            override fun matchesSafely(post: Post): Boolean {
                return withId.matches(post.id)
                        && withAuthorMail.matches(post.authorMail)
                        && withTitle.matches(post.title)
                        && withContent.matches(post.content)
                        && withUpdateTime.matches(post.updateTime)
                        && withCreationTime.matches(post.creationTime)
                        && withDeleted.matches(post.deleted)
            }
        }
    }
}