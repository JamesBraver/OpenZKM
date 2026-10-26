package com.zelix.framework.instructions;

import com.zelix.framework.analyzer.FrameNode;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class MonitorEnterNode extends AbstractMonitorNode {
	public MonitorEnterNode() {
		super(MONITORENTER);
	}

	@Override
	public void updateFrameMonitors(FrameNode frame) {
		frame.pushMonitor(this);
	}
}
