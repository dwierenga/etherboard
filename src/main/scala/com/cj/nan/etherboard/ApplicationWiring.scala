package com.cj.nan.etherboard

import com.cj.nan.etherboard.Services.{BoardService}


case class ApplicationWiring(util: utilities.Util, boardDao:BoardDao, boardService:BoardService){
  this.boardService.wiring = this
}
