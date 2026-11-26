# Debugging report — 2 documented bugs

## Bug 1 — Category filter lost after rotation / process restore

**Symptom.** In *My books*, select a category in the spinner (e.g. *Mystery*) and rotate the screen
(or come back to the app after the system killed it): the list shows **all** books again and the spinner
goes back to *All categories*.

**Investigation.**
* Breakpoint in `BookListFragment.updateCategorySpinner()` and in `BookListViewModel.setCategory()`.
* Variable inspection showed that `updateCategorySpinner` was first called with an **empty list**:
  the `categories` StateFlow was created with `stateIn(..., initialValue = emptyList())`.
* With an empty list, `indexOfFirst { it.id == selectedId }` returned `-1`, so the code treated the
  selected category as "deleted" and called `setCategory(ALL_CATEGORIES)`, overwriting the value
  restored from the `SavedStateHandle`.

**Cause.** "Not loaded yet" and "no categories" were represented by the same value (`emptyList()`).

**Fix.** The StateFlow now starts with `null` (= loading) and the fragment ignores `null`
(`BookListViewModel.categories`, `BookListFragment.onViewCreated`). The same pattern is used in
`BookEditViewModel`. The selection is now kept after rotation.

---

## Bug 2 — Wrong toolbar title on the edit screen after rotation

**Symptom.** Open a book → *Edit*: the title is "Edit book". After a rotation, the title becomes
"Add book".

**Investigation.**
* Logcat + breakpoints in `BookEditFragment.onViewCreated()` and in the
  `OnDestinationChangedListener` used by `setupActionBarWithNavController` (`MainActivity`).
* After rotation, the fragment view was re-created (title set to "Edit book") **before**
  `MainActivity.onCreate()` finished calling `setupActionBarWithNavController`, which immediately re-applied
  the destination label from `nav_graph.xml` (`@string/title_add_book`).

**Cause.** Order of execution during Activity re-creation: the Navigation label overwrote the title
set by the fragment.

**Fix.** The title is set in `BookEditFragment.onResume()`, which always runs after the Activity setup.

---

## Tools used
* **Logcat** with tags `SeedImporter` (rejected JSON records, import report), `BookRepository`
  (progress changes `page X -> Y` when sessions are added/deleted), `CategoryRepository`.
* **Breakpoints + variable inspection** in the ViewModels, repositories and `ReadingProgress`.
* **App Inspection → Database Inspector** to check the `books`, `categories` and `reading_sessions`
  tables (cascade delete, `categoryId` set to NULL).
