package it.francescogabbrielli.apps.stargazers;

import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.isA;

/**
 * Created by Francesco Gabbrielli on 26/02/18.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class EndlessScrollingTest {

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testSearch() {

        onView(withId(R.id.search))
                .perform(click())
                .perform(typeTextIntoFocusedView("rtens/mockster"));

//        onData(allOf(isA(GitHubUser.class)))
//                .inAdapterView().check(matches(contains(withText("danillos"))));

//        onView(withClassName(new BaseMatcher<String>() {
//            @Override
//            public boolean matches(Object item) {
//                return item instanceof RecyclerView;
//            }
//            @Override
//            public void describeTo(Description description) {
//                description.appendText("Recycler Scroller");
//            }
//        })).check(matches(with

    }
}
