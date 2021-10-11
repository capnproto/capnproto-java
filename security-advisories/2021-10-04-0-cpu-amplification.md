Problem
=======

Incorrect parsing of large list element sizes may lead to CPU usage amplification attack

Discovered by
=============

Martin Dindoffer &lt;contact@dindoffer.eu>

Announced
=========

2021-10-04

Impact
======

It is possible for an attacker to craft a small malicious message that will contain large lists with no data, leading to
artificial load on the system and possibly a Denial of Service attack.

CVSS score
==========

5.9 (Medium) CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H

Fixed in
========

Release 0.1.12

Details
=======

A tag pointer encodes the size of the elements in words and the total number of elements. Validation of these values is
performed in the code, but only as an overflow check of their product (against the total wordcount encoded in the list
pointer). The element size is read as a `short` value with improper casting to signed integers. Therefore, it is
possible to craft a message that will encode a large amount of elements (up to `2^30`) and offset this bloated size by
specifying a negative size of the elements (in words). There is no need to encode the actual payload for the large list,
thus capnproto-java will generate Iterators with large iteration counts over empty payload with messages of well under
100 bytes, that may put CPU under loads heavy enough to easily perform DoS attacks.

Remediation
===========

The `short` values of the list element sizes are now parsed as unsigned values, making sure that the list overflow check
will catch the huge list size.