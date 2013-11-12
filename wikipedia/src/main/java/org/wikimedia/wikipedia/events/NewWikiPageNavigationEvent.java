package org.wikimedia.wikipedia.events;

import org.wikimedia.wikipedia.PageTitle;

public class NewWikiPageNavigationEvent {
        private final PageTitle title;

        public NewWikiPageNavigationEvent(PageTitle title) {
            this.title = title;
        }

        public PageTitle getTitle() {
            return title;
        }
}