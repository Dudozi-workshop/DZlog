# iOS result photo quality

Implemented; native CI pending publication.

- Both placed and free-placement composition read the saved workspace quality mode.
- Result JPEG quality follows Android's existing values: speed 88, balanced 94, quality 99.
- Speed mode limits the result's long edge to 1800 px without enlarging small images.
- Photo and full-frame overlay scale together and encode once, preserving alignment.
- Original-role payloads keep the exact camera bytes and metadata under the existing iOS contract. Native camera capture prioritization and original-photo quality parity remain separate work.
- The existing EXIF raster UI check now also verifies encoded dimensions for all three modes, overlay alignment after downscaling, small-image preservation and untouched original input.
- Local whitespace checks only; iOS execution is delegated to accumulated native CI. No physical-device image-quality claim.
