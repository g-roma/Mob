Mob
===


Mob is a SuperCollider program that allows you to live-code a group of agents animated in a 2D surface. Each agent controls a synth. The surface can contain different kinds of data, including audio samples, which the synth can use. Agent positions are decided by functions which are live-coded the bottom panel. The agent interface also includes a slider that can be controlled via MIDI.

Requirements
------------
Mob can be used on its own (executed from SuperCollider) but the following libraries are used for some functionality:

- [Modality](https://modalityteam.github.io/) for MIDI control
- [FluidCorpusMap2](https://github.com/flucoma/FluidCorpusMap2) for sample-based terrain
- SLUGens (part of [sc3-plugins](https://github.com/supercollider/sc3-plugins)) for wave terrain synthesis.

How to use
----------
To install, copy to the SuperCollider Extensions folder.

To start, just create an instance of Mob. Different options are available in the constructor (documentation coming soon).

Some examples:

```supercollider
Mob.new(100, 100, 10)
// create an instance with a 100x100 terrain and a rate of 10 fps
Mob.new(rate:25,data: (type:"samples", value:"path/to/folder"));
// create an instance with a terrain of samples loaded via FluidCorpusMap2
Mob.new(100, 100, 10, (type:"function",value:{|x, y| abs(cos(pi*x + (5*cos(pi*y)))) }))
// create an instance with a terrain of floats, which can be used with WaveTerrain

```

After the program is initialized, double click on the terrain to create an agent. Type some code and press cmd-right-arrow to start the agent moving. The function should be something like:

```supercollider
x = x  + 1;
y = 10;
synth = \sines;
```

The x and y variables control the position of the agent. The synth definition must be defined in a file named "mob_synthdefs", by default in the Extensions folder (a folder parameter can be passed via constructor).
In the above example, the "sines" synthdef could be something like:

```supercollider
SynthDef(\sines, {|x = 0.5, y = 0.5, t = 0, a = 0, w = 100, h = 100|
	Out.ar(0, Lag.kr(a) * Lag.kr(y/h) * SinOsc.ar(100+(1000* Lag.kr(x/w)))!2);
}).add;
```

Here, x and y are updated with the position of the agent, a is the value of the slider, and w and h are the width and height of the terrain.

