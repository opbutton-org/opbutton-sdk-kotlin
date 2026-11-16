# Widgets

## Retained layout options

Elements receive generated IDs automatically. Layout edits preserve IDs; removing
or replacing a tree detaches its callbacks and providers. Serialize tree edits on
the host UI thread. The SDK ID counter itself is thread-safe.

Rows and columns support gap, padding, align, justify, wrap and inherited density.
Use a grid for equal-width commands: columns is the maximum column count, and
minCellWidth allows automatic reduction on narrow panels. Placement supports
content width, numeric width, fill with a weight, minimum/maximum width and
alignSelf. Dimensions are dashboard CSS pixels. Radio options stay in one group;
slider changes are sent on release/keyboard commit, with a numeric value.

Install the updated backend and dashboard before using these extensions. Protocol
version 2 remains compatible with existing panels. Optional public ID aliases
retain their existing meaning and are unnecessary for creating elements.

```kotlin
val root = ui.addTab("Game").vertical().gap(12.0).padding(8.0)
val commands = root.addGrid().columns(3).minCellWidth(140.0).gap(8.0)
commands.addButton("Restart")
commands.addButton("Clear logs")
val row = root.addHorizontal().gap(8.0).align("end")
row.addSelect("Language").options(arrayOf(arrayOf("en", "English"), arrayOf("ru", "Русский")))
    .defaultValue("en").fill().minWidth(160.0)
row.addButton("Apply").width(120.0)
root.addRadioGroup("Difficulty").options(arrayOf(arrayOf("easy", "Easy"), arrayOf("hard", "Hard")))
    .defaultValue("easy").optionDirection("horizontal")
root.addSlider("Speed").range(0.5, 3.0, 0.1).defaultValue(1.0).unit("x").fill()
    .onChange { action: OpButtonSliderAction -> setSpeed(action.value) }
```
