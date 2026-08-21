package atlas.presentation.home.web;

import atlas.domain.home.enums.NewsCategory;

record NewsItem(NewsCategory category, String title, String url, String publishedAt) {}
