# Required fixes

## Scanner GUI heading alignment

Status: reported, not implemented.

The Russian Scanner screen has an inconsistent header layout: the main title
and the result-area caption do not have clearly separated bounds, and the
result caption reaches or crosses the right frame. Review both headings together
rather than moving only the main title.

The supplied screenshot shows this in the idle state. In
`src/main/java/ic2/core/block/machine/gui/GuiScanner.java`, the result caption is
drawn at a fixed `(123, 6)` after the common GUI title is rendered.

Required outcome:

- Give the Scanner title and result caption separate, correctly aligned areas.
- Keep both labels inside the frame, with a visible gap and no overlap.
- Check English and Russian at supported GUI scales.
- Preserve slot positions, scanner behavior and the existing localization keys.

Only this issue has been recorded; the GUI and textures have not been changed.
