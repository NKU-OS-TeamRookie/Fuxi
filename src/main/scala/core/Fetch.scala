package core

import chisel3._

import io._
import consts.Constants._
import consts.Instructions.NOP
import bpu.BranchPredictor

class Fetch extends Module {
  val io = IO(new Bundle {
    // pipeline control signals
    val flush   = Input(Bool())
    val stall   = Input(Bool())
    val excPc   = Input(UInt(ADDR_WIDTH.W))
    // ROM interface
    val rom     = new SramIO(ADDR_WIDTH, DATA_WIDTH)
    // branch information (from decoder)
    val branch  = Input(new BranchInfoIO)
    // to next stage
    val fetch   = Output(new FetchIO)
  })

  // program counter
  val pc = RegInit(RESET_PC - (INST_WIDTH / 8).U)

  // branch predictor
  val bpu = Module(new BranchPredictor)
  bpu.io.branchInfo <> io.branch
  bpu.io.lookupPc   := pc

  // update PC
  val nextPc = Mux(io.flush, io.excPc,
               Mux(io.stall || !io.rom.valid, pc,
               Mux(bpu.io.predTaken, bpu.io.predTarget,
                   pc + (INST_WIDTH / 8).U)))
  pc := nextPc

  // generate ROM signals
  io.rom.en     := true.B
  io.rom.wen    := 0.U
  io.rom.addr   := nextPc
  io.rom.wdata  := 0.U

  // generate output
  io.fetch.inst       := Mux(io.rom.valid, io.rom.rdata, NOP)
  io.fetch.pc         := pc
  io.fetch.predIndex  := bpu.io.predIndex
}
