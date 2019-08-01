# mmbase-utils

[![Build Status](https://travis-ci.org/mmbase/mmbase-utils.svg?)](https://travis-ci.org/mmbase/mmbase-utils)
[![snapshots](https://img.shields.io/nexus/s/https/oss.sonatype.org/org.mmbase/mmbase-utils.svg)](https://oss.sonatype.org/content/repositories/staging/org/mmbase/)
[![Maven Central](https://img.shields.io/maven-central/v/org.mmbase/mmbase-utils.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.mmbase%22)


<p>
MMBase Utils is a part of MMBase, but can also be used in
other projects. It contains several kind of utility
classes.</p>
<p>Highlights:</p>
<ul>
 <li>An implementation of 'events'. Threads can use this to
  communicate certain things. Using mmbase-clustering,
  these events can also be sent to other servers.</li>
 <li>A logging framework</li>
 <li>A framework for string and byte array transformeration,
  plus a whole lot of implemetentations for that.</li>
  <li>Several java.util like classes (collections, dateparsing)</li>
  <li>File type recognition ('magicfile')</li>
  <li>The MMBase resourceloader, a generic fall-back mechanism
  for configuration files and similar resources.</li>
  <li>...</li>
</ul>
