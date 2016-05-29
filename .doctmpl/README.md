# akka-visualmailbox

[![Build Status](https://travis-ci.org/ouven/akka-visualmailbox.svg?branch=master)](https://travis-ci.org/ouven/akka-visualmailbox)
[![Project Score](https://img.shields.io/badge/Project%20Score-%F0%9F%92%A9-brightgreen.svg)](https://img.shields.io)
[![Project Mood](https://img.shields.io/badge/Project%20Mood-%F0%9F%98%84-brightgreen.svg)](https://img.shields.io)

Current version: ${version}

This project wants help you finding hotspots in you Akka application by visualizing you message flows.

![sample flow](./sample.png)

## collector

akka.actor.default-mailbox.mailbox-type = "de.aktey.akka.visualmailbox.VisualMailboxType"


## visualization
JavaScript uses the "class" keyword and "EventSource", so it is viewable with Chrome
42+ or Firefox 45+. Other browsers I did not try.

## common