Mob {
	var <>width, <>height, <rate;
	var folder, buttonGroup, sliderGroup;
	var <window, <>surface, <tabs;
	var agents, terrainType, terrainBuffer, <>patchVals;
	var server, scale;
	var <terrain;


	*new{|width = 100, height = 100, rate = 60, data = nil, folder = nil, buttonGroup = nil, sliderGroup = nil|
		^super.newCopyArgs(width, height, rate, folder, buttonGroup, sliderGroup).init(data);
    }

	init{|data = nil|
		if (folder.isNil) {folder = Platform.userExtensionDir};
		thisProcess.interpreter.executeFile(
			folder++"/mob_funcs.scd"
		);
		server = Server.default;
		agents = [];
		patchVals = [];
		if (data.isNil){
			terrainType = "blank";
			this.makeUI;
		}{
			data[\type].switch(
				"function", {this.loadFunction(data[\value])},
				"file", {this.loadFile(data[\value])},
				"samples", {this.loadSamples(data[\value])}
			)
		};

		if(buttonGroup.notNil && sliderGroup.notNil) {this.setupMIDI};
		terrain = MobTerrain.new;
		server.options.numBuffers = 100000;
		server.options.numOutputBusChannels = 4;//?
		server.waitForBoot{
			if(File.exists(folder++"/mob_synthdefs.scd";)){
				thisProcess.interpreter.executeFile(folder++"/mob_synthdefs.scd";)
			};
		}
	}

	loadFunction{|f|
		var terrain;
		terrain = Array.fill(width * height,{|i|
			var x = i % width;
			var y = (i - x).div(width);
			f.value(x / width, y / height);
		});
		terrain = terrain.normalize;
		patchVals = terrain.reshape(height, width);
		server.waitForBoot{
				terrainBuffer = Buffer.alloc(server, terrain.size, bufnum: 0);
			    terrain = 2 * terrain;
			    terrainBuffer.loadCollection(terrain, action:{
		     })
		 };
		terrainType = "float";
		this.makeUI;
	}

	loadFile{|path|
		var data, header;
		terrainType = "int";
		if (File.exists(path).not){
			"file not found".postln;
		}{
			data = FileReader.read(path).postcs;
			header = data[0];
			width = header[0].asInteger;
			height = header[1].asInteger;
			terrainType = header[2];
			patchVals = data[1..].collect {|line|
				line.collect {|v|
					terrainType.switch(
						"bool", v.asInteger.asBoolean,
						"int", v.asInteger,
						"float", v.asFloat
					)
				}
			}
		};
		this.makeUI;
	}

	loadSamples{|path, doneFunc|
		var inst, fcm, data, w, h, type;
		var n = PathName(path).files.size;
		terrainType = "wave";
		h = n.sqrt.asInteger;
		w = h + ((n - (h * h)) / h).ceil.asInteger;
		fcm = FCM.new(server, 100000)
		    .addFolder(path)
		    .makeIndex()
            .run{
	          defer{
				fcm.mapDS.dump{|ds|
					width = ds["data"].collect{|x| x[0]}.maxItem.asInteger + 1;
					height = ds["data"].collect{|x| x[1]}.maxItem.asInteger + 1;
					patchVals = Array.fill(width, {Array.fill(height, 0)});

					ds["data"].keysValuesDo{|k, v|
						var x = v[0].asInteger;
						var y = v[1].asInteger;
						patchVals[x][y] = fcm.sounds[k.asInteger].buffer.bufnum;
					};
					defer{
						this.makeWindow;
						surface = FCMPlotView.new(window, window.bounds, fcm);
						surface.background_(Color.red);
						this.setupSurface;
						this.makeLayout;
						surface.size =  (surface.bounds.width / width).asInteger;
					}
				};
		       }
		      };
	}


	setupMIDI{
		8.do{|i|
			buttonGroup.elAt(i).value = 0;
			buttonGroup.elAt(i).action = {
				defer{
					if(tabs.tabViews.size > i){
						tabs.tabViews.size.do{|j|buttonGroup.elAt(j).value  = 0};
						tabs.focus(i);
						buttonGroup.elAt(i).value = 1;
					}
				}
			};

			sliderGroup.elAt(i).action = {|el|
				defer{
					if(agents.size > i){
						agents[i].slider.value = el.value;
					}
				}
			}
		}
	}

	remove{|agent|
		agents.remove(agent);
		agents.size.postln;
	}

	calcBounds{
		var wHeight = Window.screenBounds.height;
		var surfaceHeight = 4 * (wHeight - 28) / 5;
		var targetWindowWidth = surfaceHeight + 22;
		^Rect(0, 0, targetWindowWidth, wHeight);
	}

	makeUI{
		this.makeWindow;
		this.setupSurface;
		this.makeLayout;
	}

	setupSurface{
		if(surface.isNil){
			surface = UserView.new(window, Rect(0,0,300,300)).drawFunc_{|v| this.draw(v)}
		}{
			surface.addDrawFunc{|v| this.draw(v)}
		};
		surface.background_(Color.black)
		.animate_(true).frameRate_(rate)
		.mouseDownAction_{|v, x, y, m, b, c|
			if(c == 2){
				agents = agents.add(MobAgent.new(this, x / scale.x, y / scale.y));
				tabs.focus(agents.size-1);
			};
		}
		.mouseMoveAction_{|v, x, y, m, b, c|
			if(m == 131072){
				agents[tabs.activeTab.index].pos = (x@y)/scale;
				((x@y)/scale).postln;
			}
		};

	}

	makeWindow{
		QtGUI.palette = QPalette.dark;
		window = Window.new("mob");
		window.bounds = this.calcBounds;
		window.bounds.postln;
		window.bounds.top = -500;
		tabs = TabbedView2.new(window);
	}

	makeLayout{
		window.layout = VLayout(
        [surface, s:4],
        [tabs.view, s:1]
		);
		window.front;
	}

	draw{|view|
		var selected = 0;
		if(agents.size >0){selected = tabs.activeTab.index};

		scale = (view.bounds.width / width) @ (view.bounds.height / height);

		if(terrainType!="wave"){
			width.do{|col|
				height.do{|row|
					var pos = col@row * scale;
					Pen.color = terrainType.switch(
						"float", {
							Color.grey(patchVals[row][col])
						},
						"bool", {
							patchVals[row][col].if({Color.white}, {Color.black})
						},
						"int",{
							Color.hsv(patchVals[row][col]/10.0, 1, 1);
					    }
					);
					Pen.addRect(Rect(pos.x, pos.y, scale.x, scale.y));
					Pen.fill;
			}}
		};

		agents.do{|agent, i|
			var pos, color;
			agent.execFunc;
			pos = agent.pos * scale;
			color = agent.color.copy;
			color.alpha = agent.slider.value;
			Pen.addRect(Rect(pos.x, pos.y, scale.x, scale.y));
			Pen.color = color;

			Pen.fill;
			if(selected == i){
				Pen.addRect(Rect(pos.x, pos.y, scale.x, scale.y));
				Pen.strokeColor = Color.white;
				Pen.stroke

			};
		}
	}
}

MobTerrain{
	var mob, <>width, <>height;
	var <>values;
	*new{|mob, w = 0, h = 0|
		^super.newCopyArgs(mob, w, h).init;
	}
	init{
		values = Dictionary.new;
		if (~terrain_funcs.notNil){
			~terrain_funcs.keysValuesDo{|k, v|
				this.addUniqueMethod(k, v);
			}
		}
	}
}


