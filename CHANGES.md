# 0.1.6 -- UNRELEASED

The RiemannAdapter can now be configured to use TCP, with the new `tcp` property.
The default (for now) is to continue using UDP.

Incompatible changes:

Some properties of RiemannAdapter have changed from string to an appropriate type
(int or boolean); 