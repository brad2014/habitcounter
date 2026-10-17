# HabitCounter

A git repo for a minimal Android App that easily allows me to record a habitual
action, and later shows me a graph of actions per hour or acts per day. It could
be a symptom like a cough, it could be drinking a glass of water, it could be
counting people who enter a door. The design goal is that it be quick and easy
to note an instance of the action, recording a timestamped "click".

## Main Page

The app main page shows a list of counters and for each its count total number
for current month, current day, current hour, an increment icon (+), an edit
icon (pen).

At the top fo the list is a "New counter" button which goes to the counter edit
page to add a new counter. When the edit icon is pressed for a given counter,
the counter edit page for that counter is presented.

When the increment icon is pressed for a given counter, a timestamp is recorded
in the history array for the counter, and the totals for this counter row are
updated.

When any counter number is pressed, the graph page for that counter and that
interval is displayed.

## Counter Edit Page

The page to add a button (or edit its settings) allows the user to set the label
and pick a color for the counter. Also checkbox to "pin to desktop". Also a
button to "reset counter" (discarding count history, reinitializing graph). Also
delete button to remove the counter. Also a "save" button that returns to main
screen. Also a "back" button that returns to the main screen, or, if there are
unsaved edits, displays a "Save edits?" dialog with "Discard edits" and "Save
edits" buttons.

When a counter is pinned to desktop, the icon for the app is shaded using the
color, labeled, and displayed on the desktop. Tapping that button simply
increments the counter. The past-hour count is displayed in a bubble under the
desktop icon.

The user can pin or unpin multiple counters at will by editing their settings.

## Counter Graph Page

When any such number is pressed, a page is displayed that graphs the total at
that interval (by month for past 12 months, by day for past 31 days, by hour for
past 24 hours), with a "back" button. Also a "count settings" button to edit the
counter settings.
