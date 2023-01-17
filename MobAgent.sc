MobAgent{
	var mob, <>pos, <time, <color, <synth, <>data, synthDef, tabView, textView, <slider;

	var template, compiled, lastCompiled;

	*new{|mob, x = 0, y = 0|
		^super.new.init(mob, x, y);
	}

	keyDown{|view, char, mod, unicode, keycode|
		if(mod == 3145728){
			keycode.switch(
				124, {this.compile},
				/*123, {this.stop}*/
			);
		};
			if(textView.string != lastCompiled){
				textView.stringColor_(Color.grey(0.5));
			}{
				textView.stringColor_(Color.grey(0.7));
			};
	}

	compile{
		var result = template.format(textView.string).compile.value;
		if(result.notNil){
			compiled = result;
			lastCompiled = textView.string;
		}
	}


	init{|aMob, x = 0, y= 0|
		mob = aMob;
		color = Color.rand;
		tabView = mob.tabs.add("").closable_(true).onRemove_{|t|
			this.stop;mob.remove(this);
		};
		//tabView.background = color;
		tabView.labelColor = color;
		tabView.unfocusedColor = color.blend(Color.white, 0.5);
		textView = TextView(tabView)
		  .font_(Font("Inconsolata",18))
		  .keyDownAction_{|v,c,m,u,k| this.keyDown(v,c,m,u,k)};

		slider = Slider(tabView);

		tabView.view.layout = HLayout([textView, s:5], [slider, s:1]);
		pos = x@y;
		x.addUniqueMethod(\wrapxy, {|z| z%mob.width});
		y.addUniqueMethod(\wrapxy, {|z| z%mob.height});
		time = 0;
		template = "{|x,y,t,a,p, synth| % [x,y, t, p, synth]}";

		if(~agent_funcs.notNil) {
			~agent_funcs.keysValuesDo{|k, v|
				this.addUniqueMethod(k, v);
			}
		}

	}

	stop{
		synth.free;
	}

	update{|compiled|
		var result, newPos, newSynthDef;
		result = compiled.value(pos.x, pos.y, time, slider.value, mob.terrain);
		newPos = result[0] @ result[1];
		newPos.x = newPos.x % mob.width;
		newPos.y = newPos.y % mob.height;
		pos = newPos;
		time = time + 1;
		newSynthDef = result[4];
		if (newSynthDef != synthDef){
			synth.free;
			synth = Synth(newSynthDef,
				args:[\x, pos.x, \y, pos.y, \t, time, \a, slider.value, \w, mob.width, \h, mob.height]);
			synthDef = newSynthDef;
		};
	}

	execFunc{
		if(compiled.notNil){
			this.update(compiled);
		};
		synth.set(\x, pos.x );
		synth.set(\y, pos.y );
		synth.set(\t, time);


		if (mob.patchVals.notNil && mob.patchVals.isEmpty.not,{
			synth.set(\p, mob.patchVals[pos.x][pos.y]);});
		synth.set(\a, slider.value);

	}
}
