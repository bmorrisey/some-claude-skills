# claude-skills

A collection of custom [Claude Code](https://claude.ai/code) skills — reusable, slash-command-style agents that encode hard-won domain knowledge so you don't have to re-derive it every project.

## What is a skill?

Skills are self-contained instruction sets that Claude Code can load on demand. Each skill lives in its own directory and contains a `SKILL.md` that tells Claude what it does, when to use it, and how to execute it — along with any templates, checklists, or reference docs it needs.

## Skills

| Skill | Description |
|---|---|
| [`start-atak-plugin`](start-atak-plugin/SKILL.md) | Bootstrap a new ATAK CIV SDK plugin from scratch — project skeleton, build.gradle, plugin.xml, AndroidManifest.xml, CI workflows, and TPC release packaging. Targets SDK 5.5+ (IPlugin style). Templates encode fixes from a shipped, production plugin. |

## Usage

Skills are invoked by name within a Claude Code session. If Claude Code is configured to load this repo's skills, type `/start-atak-plugin` (or describe what you want) and Claude will find and execute the matching skill.

To wire up skills from a local directory, add the path to your Claude Code settings under `skillsDirectories`.
