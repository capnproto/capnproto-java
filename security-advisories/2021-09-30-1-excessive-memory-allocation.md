Problem
=======

Insufficient validation of message metadata with negative segment sizes may lead to excessive memory allocation and
subsequent DoS.

Discovered by
=============

Martin Dindoffer &lt;contact@dindoffer.eu>

Announced
=========

2021-09-30

Impact
======

It is possible for an attacker to craft a malicious message that may thanks to memory amplification lead to a Denial of
Service attack against the consumer of CapnProto messages. In practical terms a message of only 8 bytes may cause
allocation of 2GB of memory.

CVSS score
==========

7.5 (High) CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H

Fixed in
========

Release 0.1.11

Details
=======

A read of 4 bytes from the message is performed, the result of which is read as the size of a segment in a variable.
This size is stored in memory as a signed integer. If the segment count is 1, the size of the first segment is also
considered to be the total size of the message stored in a totalWords variable. Since both the segment0Size and the
totalWords are signed integers, they may hold a negative number which will pass the traversal limit check.

Subsequently capnproto-java tries to allocate enough memory for the segment. Because the encoded size is in words, the
value is multiplied by the number 8. The resulting number is used as the size of a ByteBuffer allocated for the first
segment. A sufficiently large negative number when multiplied by 8 will net a large positive integer, thus causing large
memory allocation in the form of a ByteBuffer.

A segment size of `-1 879 048 193` multiplied by 8 results in `2 147 483 640` bytes of allocated memory, which is close
to `Integer.MAX_VALUE` and around the size of the maximum length of a Java array. Therefore the maximum size a message
may allocate this way is roughly 2GB.

Remediation
===========

Stricter validation refusing negative segment sizes was implemented.