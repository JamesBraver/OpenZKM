package com.zelix.framework.instructions;

import com.zelix.framework.analyzer.FrameNode;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class MonitorExitNode extends AbstractMonitorNode {
	public MonitorExitNode() {
		super(MONITOREXIT);
	}

	@Override
	public void updateFrameMonitors(FrameNode frame) {
		frame.popMonitor(this);
	}
}
