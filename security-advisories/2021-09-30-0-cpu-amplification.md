Problem
=======

CPU usage amplification attack

Discovered by
=============

Martin Dindoffer &lt;contact@dindoffer.eu>

Announced
=========

2021-09-30

Impact
======

It is possible for an attacker to craft a small malicious message that will contain large lists with no data, leading to
artificial load on the system and possibly a Denial of Service attack.

CVSS score
==========

5.9 (Medium) CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H

Fixed in
========

Release 0.1.11

Details
=======

Cap'n'Proto List pointer encodes the size of the list in the header as a 29 bit value. This value is read as a counter
for the list Iterator. Setting a large value for the elementCount may produce empty loops in the code that don't operate
on any data but still consume CPU time.

List Amplification is a well known posibility in the Cap'n'Proto protocol, as can be seen in the Encoding
spec (https://capnproto.org/encoding.html):
> A list of Void values or zero-size structs can have a very large element count while taking constant space on the wire.
> If the receiving application expects a list of structs, it will see these zero-sized elements as valid structs set to their default values.
> If it iterates through the list processing each element, it could spend a large amount of CPU time or other resources despite the message being small.
> To defend against this, the “traversal limit” should count a list of zero-sized elements as if each element were one word instead.
> This rule was introduced in the C++ implementation in commit 1048706.

A form of this traversal limit countermeasure is present in capnproto-java. However, the message may contain a huge list
with 1-bit elements that are read as a struct list. This combined with the fact that the data may simply be stripped
from the message makes it is possible to create problematically huge Iterators with default traversal limits.

Remediation
===========

The iteration should take into account the actual message length. As a rule of thumb the parser should not enter loops
with an attacker-controlled iteration count that is not bounded by the input length.

Therefore additional bounds checking was introduced to detect out-of-bounds list pointers.